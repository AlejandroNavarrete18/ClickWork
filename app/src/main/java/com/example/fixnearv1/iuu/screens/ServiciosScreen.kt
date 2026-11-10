package com.example.fixnearv1.iuu.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.PerfilUsuario
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiciosScreen(
    onRegresar: () -> Unit,
    onVerPerfilTrabajador: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var busqueda by remember {
        mutableStateOf("")
    }

    var oficioSeleccionado by remember {
        mutableStateOf("Todos")
    }

    var perfilCliente by remember {
        mutableStateOf<PerfilUsuario?>(null)
    }

    var trabajadores by remember {
        mutableStateOf<List<PerfilUsuario>>(emptyList())
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    var mensajeDialogo by remember {
        mutableStateOf("")
    }

    var tituloDialogo by remember {
        mutableStateOf("")
    }

    var trabajadorEnviandoId by remember {
        mutableStateOf("")
    }

    var trabajadorPendientePermiso by remember {
        mutableStateOf<PerfilUsuario?>(null)
    }

    // Trabajador al que se le va a escribir una solicitud (abre el diálogo)
    var trabajadorParaSolicitud by remember {
        mutableStateOf<PerfilUsuario?>(null)
    }

    var textoSolicitud by remember {
        mutableStateOf("")
    }

    // Lo que el CLIENTE necesita (antes se enviaba la descripción del trabajador)
    var descripcionSolicitud by remember {
        mutableStateOf("")
    }

    fun enviarSolicitudReal(trabajador: PerfilUsuario) {
        val cliente = perfilCliente

        if (cliente == null) {
            tituloDialogo = "Error"
            mensajeDialogo = "No se pudo cargar tu perfil de usuario."
            return
        }

        val userId = SesionLocal.obtenerUserId(context)
        val token = SesionLocal.obtenerAccessToken(context)

        if (userId.isBlank() || token.isBlank()) {
            tituloDialogo = "Sesión no válida"
            mensajeDialogo = "Inicia sesión nuevamente."
            return
        }

        val ubicacion = obtenerUltimaUbicacionReal(context)

        if (ubicacion == null) {
            tituloDialogo = "Ubicación no disponible"
            mensajeDialogo =
                "Activa la ubicación del dispositivo y vuelve a intentarlo."
            return
        }

        trabajadorEnviandoId = trabajador.id

        scope.launch {
            val servicio = trabajador.oficio
                .takeIf { it.isNotBlank() }
                ?: "Servicio general"

            val descripcion = descripcionSolicitud
                .ifBlank { "Solicitud enviada desde Servicios Cercanos." }

            val resultado = SupabaseApi.crearSolicitud(
                accessToken = token,
                trabajadorId = trabajador.id,
                servicio = servicio,
                descripcion = descripcion,
                latitudCliente = ubicacion.latitude,
                longitudCliente = ubicacion.longitude,
                direccionCliente = "Ubicación GPS: ${ubicacion.latitude}, ${ubicacion.longitude}"
            )

            trabajadorEnviandoId = ""

            resultado.onSuccess {
                tituloDialogo = "Solicitud enviada"
                mensajeDialogo =
                    "Tu solicitud fue enviada a ${trabajador.nombre}. " +
                            "Sigue la cotización en Inicio > Mis solicitudes."
            }

            resultado.onFailure { exception ->
                tituloDialogo = "No se pudo enviar"
                mensajeDialogo =
                    exception.message ?: "Ocurrió un error al crear la solicitud."
            }
        }
    }

    val permisosUbicacionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permisos ->
            val concedido =
                permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                        permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (concedido) {
                trabajadorPendientePermiso?.let { trabajador ->
                    enviarSolicitudReal(trabajador)
                }
            } else {
                tituloDialogo = "Permiso requerido"
                mensajeDialogo =
                    "Necesitas permitir la ubicación para crear una solicitud real."
            }

            trabajadorPendientePermiso = null
        }

    fun solicitarServicio(trabajador: PerfilUsuario) {
        if (!tienePermisoUbicacion(context)) {
            trabajadorPendientePermiso = trabajador

            permisosUbicacionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )

            return
        }

        enviarSolicitudReal(trabajador)
    }

    LaunchedEffect(Unit) {
        val userId = SesionLocal.obtenerUserId(context)
        val token = SesionLocal.obtenerAccessToken(context)

        if (userId.isBlank() || token.isBlank()) {
            error = "No hay sesión activa."
            cargando = false
            return@LaunchedEffect
        }

        val perfilResultado = SupabaseApi.obtenerPerfil(
            userId = userId,
            accessToken = token
        )

        perfilResultado.onSuccess {
            perfilCliente = it
        }

        perfilResultado.onFailure {
            error = it.message ?: "No se pudo cargar tu perfil."
        }

        val trabajadoresResultado =
            SupabaseApi.obtenerTrabajadoresDisponibles(token)

        trabajadoresResultado.onSuccess { lista ->
            trabajadores = lista.filter { trabajador ->
                trabajador.id != userId
            }
        }

        trabajadoresResultado.onFailure {
            error = it.message ?: "No se pudieron cargar trabajadores."
        }

        cargando = false
    }

    val oficiosDisponibles = trabajadores
        .map { it.oficio.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val trabajadoresFiltrados = trabajadores.filter { trabajador ->
        val texto = "${trabajador.nombre} ${trabajador.oficio} ${trabajador.descripcion}"
        val coincideTexto = texto.contains(busqueda, ignoreCase = true)
        val coincideOficio = oficioSeleccionado == "Todos" ||
                trabajador.oficio.trim().equals(oficioSeleccionado, ignoreCase = true)
        coincideTexto && coincideOficio
    }

    Scaffold(
        containerColor = FondoPrincipal,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Buscar servicio",
                        color = TextoPrincipal,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onRegresar) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = TextoPrincipal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FondoPrincipal
                )
            )
        }
    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(FondoPrincipal),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                            .border(
                                width = 1.dp,
                                color = BordeSuave,
                                shape = RoundedCornerShape(24.dp)
                            )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(24.dp),
                            color = CardOscura
                        ) {
                            Box {
                                ClickWorkMiniMap(
                                    workers = trabajadoresFiltrados,
                                    showJobs = false,
                                    showWorkers = true,
                                    onWorkerSelected = { trabajador -> onVerPerfilTrabajador(trabajador.id) },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        OutlinedTextField(
                            value = busqueda,
                            onValueChange = {
                                busqueda = it
                            },
                            placeholder = {
                                Text(
                                    "Buscar trabajador o servicio...",
                                    color = TextoSuave
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MoradoClaro
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .align(Alignment.TopCenter),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = FondoSecundario,
                                unfocusedContainerColor = FondoSecundario,
                                focusedBorderColor = MoradoPrincipal,
                                unfocusedBorderColor = BordeSuave,
                                focusedTextColor = TextoPrincipal,
                                unfocusedTextColor = TextoPrincipal,
                                cursorColor = MoradoPrincipal
                            ),
                            singleLine = true
                        )

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(50.dp),
                            color = CardOscura2,
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 18.dp,
                                    vertical = 10.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = AzulAcento
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Text(
                                    "${trabajadoresFiltrados.size} profesionales cerca",
                                    color = TextoPrincipal,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    if (oficiosDisponibles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Todos") + oficiosDisponibles) { oficio ->
                                FilterChip(
                                    selected = oficioSeleccionado == oficio,
                                    onClick = { oficioSeleccionado = oficio },
                                    label = { Text(oficio, fontWeight = FontWeight.SemiBold) },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = CardOscura,
                                        labelColor = TextoSecundario,
                                        selectedContainerColor = MoradoPrincipal,
                                        selectedLabelColor = Color.White
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = oficioSeleccionado == oficio,
                                        borderColor = BordeSuave,
                                        selectedBorderColor = MoradoPrincipal
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Profesionales disponibles",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextoPrincipal
                    )

                    Text(
                        text = "Pide una cotización sin compromiso.",
                        color = TextoSuave,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (cargando) {
                        Text(
                            text = "Cargando trabajadores...",
                            color = TextoSecundario
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (error.isNotBlank()) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (!cargando && trabajadoresFiltrados.isEmpty()) {
                        Text(
                            text = "No hay trabajadores disponibles por el momento.",
                            color = TextoSecundario
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            items(
                items = trabajadoresFiltrados,
                key = { it.id }
            ) { trabajador ->
                ServicioCardMapaMejorada(
                    trabajador = trabajador,
                    enviando = trabajadorEnviandoId == trabajador.id,
                    onVerPerfil = {
                        onVerPerfilTrabajador(trabajador.id)
                    },
                    onMapa = {
                        val consulta = Uri.encode(
                            "${trabajador.nombre} ${trabajador.oficio}"
                        )

                        val uri = Uri.parse("geo:0,0?q=$consulta")
                        val intent = Intent(Intent.ACTION_VIEW, uri)

                        runCatching {
                            context.startActivity(intent)
                        }
                    },
                    onSolicitar = {
                        textoSolicitud = ""
                        trabajadorParaSolicitud = trabajador
                    }
                )
            }
        }
    }

    trabajadorParaSolicitud?.let { trabajador ->
        AlertDialog(
            onDismissRequest = { trabajadorParaSolicitud = null },
            containerColor = CardOscura,
            titleContentColor = TextoPrincipal,
            textContentColor = TextoSecundario,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Solicitar a ${trabajador.nombre}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Cuéntale qué necesitas. Te responderá con una cotización.")
                    OutlinedTextField(
                        value = textoSolicitud,
                        onValueChange = { textoSolicitud = it.take(500) },
                        label = { Text("¿Qué necesitas?") },
                        minLines = 3,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FondoSecundario,
                            unfocusedContainerColor = FondoSecundario,
                            focusedBorderColor = MoradoPrincipal,
                            unfocusedBorderColor = BordeSuave,
                            focusedTextColor = TextoPrincipal,
                            unfocusedTextColor = TextoPrincipal,
                            focusedLabelColor = MoradoClaro,
                            unfocusedLabelColor = TextoSuave,
                            cursorColor = MoradoPrincipal
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (textoSolicitud.trim().length < 5) {
                        tituloDialogo = "Falta información"
                        mensajeDialogo = "Describe lo que necesitas (mínimo 5 caracteres)."
                    } else {
                        descripcionSolicitud = textoSolicitud.trim()
                        trabajadorParaSolicitud = null
                        solicitarServicio(trabajador)
                    }
                }) { Text("Enviar solicitud", color = MoradoClaro, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { trabajadorParaSolicitud = null }) { Text("Cancelar", color = TextoSuave) }
            }
        )
    }

    if (mensajeDialogo.isNotBlank()) {
        AlertDialog(
            onDismissRequest = {
                mensajeDialogo = ""
                tituloDialogo = ""
            },
            containerColor = CardOscura,
            title = {
                Text(
                    tituloDialogo,
                    color = TextoPrincipal
                )
            },
            text = {
                Text(
                    mensajeDialogo,
                    color = TextoSecundario
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mensajeDialogo = ""
                        tituloDialogo = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoradoPrincipal
                    )
                ) {
                    Text("Aceptar", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun ServicioCardMapaMejorada(
    trabajador: PerfilUsuario,
    enviando: Boolean,
    onVerPerfil: () -> Unit,
    onMapa: () -> Unit,
    onSolicitar: () -> Unit
) {
    val oficio = trabajador.oficio
        .takeIf { it.isNotBlank() }
        ?: "Servicio general"

    val descripcion = trabajador.descripcion
        .takeIf { it.isNotBlank() }
        ?: "Profesional disponible para solicitudes."

    val inicial = trabajador.nombre.trim().firstOrNull()?.uppercase() ?: "?"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardOscura),
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            Brush.linearGradient(listOf(MoradoSecundario, AzulAcento)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inicial,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trabajador.nombre,
                        color = TextoPrincipal,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = oficio,
                        color = MoradoClaro,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Exito.copy(alpha = 0.14f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(Exito, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            "Disponible",
                            color = Exito,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = descripcion,
                color = TextoSecundario,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Verified,
                    contentDescription = null,
                    tint = AzulAcento,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    "Perfil verificado",
                    color = AzulAcento,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onVerPerfil,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BordeSuave),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
                ) {
                    Text("Perfil", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onMapa,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BordeSuave),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
                ) {
                    Text("Mapa", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onSolicitar,
                    enabled = !enviando,
                    modifier = Modifier.weight(1.4f),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoradoPrincipal,
                        contentColor = Color.White,
                        disabledContainerColor = MoradoPrincipal.copy(alpha = 0.5f)
                    )
                ) {
                    if (enviando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Solicitar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun tienePermisoUbicacion(
    context: Context
): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun obtenerUltimaUbicacionReal(
    context: Context
): Location? {
    if (!tienePermisoUbicacion(context)) {
        return null
    }

    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val proveedores = locationManager.getProviders(true)

    return proveedores
        .mapNotNull { proveedor ->
            runCatching {
                locationManager.getLastKnownLocation(proveedor)
            }.getOrNull()
        }
        .maxByOrNull { ubicacion ->
            ubicacion.time
        }
}