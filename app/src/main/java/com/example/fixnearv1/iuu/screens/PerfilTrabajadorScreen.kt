package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.fixnearv1.utils.PerfilUsuario
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import com.example.fixnearv1.modelo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilTrabajadorScreen(
    trabajadorId: String,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current

    var perfil by remember {
        mutableStateOf<PerfilUsuario?>(null)
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    LaunchedEffect(trabajadorId) {
        val token = SesionLocal.obtenerAccessToken(context)

        if (trabajadorId.isBlank() || token.isBlank()) {
            error = "No se pudo identificar al trabajador."
            cargando = false
            return@LaunchedEffect
        }

        val resultado = SupabaseApi.obtenerPerfilPublico(
            trabajadorId = trabajadorId,
            accessToken = token
        )

        resultado.onSuccess {
            perfil = it
        }

        resultado.onFailure {
            error = it.message ?: "No se pudo cargar el perfil del trabajador."
        }

        cargando = false
    }

    val nombre = perfil?.nombre
        ?.takeIf { it.isNotBlank() }
        ?: "Trabajador"

    val oficio = perfil?.oficio
        ?.takeIf { it.isNotBlank() }
        ?: "Servicios generales"

    val descripcion = perfil?.descripcion
        ?.takeIf { it.isNotBlank() }
        ?: "Trabajador disponible para solicitudes."

    val telefono = perfil?.telefono
        ?.takeIf { it.isNotBlank() }
        ?: "No registrado"

    val correo = perfil?.correo
        ?.takeIf { it.isNotBlank() }
        ?: "No registrado"

    val disponible = perfil?.disponible == true

    Scaffold(
        containerColor = FondoPrincipal,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Perfil del trabajador",
                        color = TextoPrincipal
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onRegresar) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MoradoPrincipal
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
                .background(FondoPrincipal)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 30.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier.size(105.dp),
                    shape = CircleShape,
                    color = Color(0xFFDCE8F8)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Foto del trabajador",
                            tint = MoradoPrincipal,
                            modifier = Modifier.size(88.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (cargando) "Cargando..." else nombre,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextoPrincipal
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = oficio,
                    color = TextoSecundario
                )

                Spacer(modifier = Modifier.height(10.dp))

                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text = if (disponible) {
                                "Trabajador disponible"
                            } else {
                                "No disponible"
                            },
                            color = TextoPrincipal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = AzulAcento
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFDCE8F8),
                        labelColor = MoradoPrincipal
                    )
                )

                if (error.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                DatoTrabajadorCard(
                    titulo = "Especialidad",
                    dato = oficio
                )

                DatoTrabajadorCard(
                    titulo = "Descripción",
                    dato = descripcion
                )

                DatoTrabajadorCard(
                    titulo = "Contacto",
                    dato = "Su teléfono se comparte cuando confirmes el servicio."
                )

                DatoTrabajadorCard(
                    titulo = "Estado",
                    dato = if (disponible) {
                        "Disponible para recibir solicitudes"
                    } else {
                        "Fuera de servicio"
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        onRegresar()
                    },
                    enabled = disponible && !cargando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MoradoPrincipal,
                        disabledContainerColor = Color(0xFF7482A0)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Work,
                        contentDescription = null,
                        tint = MoradoPrincipal
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Regresar para solicitar",
                        color = TextoPrincipal
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRegresar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MoradoPrincipal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text("Volver a servicios")
                }
            }
        }
    }
}

@Composable
fun DatoTrabajadorCard(
    titulo: String,
    dato: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.labelMedium,
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = dato,
                style = MaterialTheme.typography.bodyLarge,
                color = TextoPrincipal
            )
        }
    }
}