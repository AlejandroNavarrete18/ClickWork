package com.example.fixnearv1.iuu.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import kotlinx.coroutines.launch

private val OficiosSugeridos = listOf(
    "Plomería", "Electricidad", "Limpieza", "Jardinería", "Carpintería",
    "Pintura", "Mecánica", "Tecnología", "Eventos", "Otros"
)

/**
 * Cualquier usuario puede ofrecer servicios sin cambiar de cuenta.
 * Para aparecer en Servicios y en el mapa necesita oficio, descripción y ubicación.
 */
@Composable
fun ModoTrabajadorScreen(onRegresar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var activo by remember { mutableStateOf(false) }
    var disponible by remember { mutableStateOf(false) }
    var oficio by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var latitud by remember { mutableStateOf<Double?>(null) }
    var longitud by remember { mutableStateOf<Double?>(null) }
    var ubicacionNueva by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var cerrarAlAceptar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val userId = SesionLocal.obtenerUserId(context)
        val token = SesionLocal.obtenerAccessToken(context)
        if (userId.isBlank() || token.isBlank()) {
            mensaje = "No hay sesión activa."
            cargando = false
            return@LaunchedEffect
        }
        val resultado = SupabaseApi.obtenerPerfil(userId = userId, accessToken = token)
        resultado.onSuccess { p ->
            activo = p.ofreceServicios
            disponible = p.disponible && p.ofreceServicios
            oficio = p.oficio
            descripcion = p.descripcion
            latitud = p.latitud
            longitud = p.longitud
        }
        resultado.onFailure { mensaje = it.message ?: "No se pudo cargar tu perfil." }
        cargando = false
    }

    fun capturarUbicacion() {
        val ubicacion = ubicacionActual(context)
        if (ubicacion == null) {
            mensaje = "No se pudo obtener tu ubicación. Activa el GPS e inténtalo de nuevo."
        } else {
            latitud = ubicacion.latitude
            longitud = ubicacion.longitude
            ubicacionNueva = true
        }
    }

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) capturarUbicacion()
        else mensaje = "Sin permiso de ubicación no podemos mostrarte en el mapa."
    }

    fun guardar() {
        val oficioLimpio = oficio.trim()
        val descripcionLimpia = descripcion.trim()

        if (activo) {
            if (oficioLimpio.length < 3) {
                mensaje = "Indica a qué te dedicas (mínimo 3 caracteres)."
                return
            }
            if (descripcionLimpia.length < 10) {
                mensaje = "Describe tu servicio con al menos 10 caracteres."
                return
            }
            if (disponible && (latitud == null || longitud == null)) {
                mensaje = "Para aparecer como disponible necesitas guardar tu ubicación."
                return
            }
        }

        scope.launch {
            val userId = SesionLocal.obtenerUserId(context)
            val token = SesionLocal.obtenerAccessToken(context)
            guardando = true
            val resultado = SupabaseApi.actualizarModoTrabajador(
                accessToken = token,
                userId = userId,
                ofreceServicios = activo,
                disponible = activo && disponible,
                oficio = oficioLimpio,
                descripcion = descripcionLimpia,
                latitud = if (ubicacionNueva) latitud else null,
                longitud = if (ubicacionNueva) longitud else null
            )
            guardando = false
            resultado.onSuccess {
                cerrarAlAceptar = true
                mensaje = if (activo) {
                    "Listo. Los clientes ya pueden encontrarte en Servicios."
                } else {
                    "Modo trabajador desactivado."
                }
            }
            resultado.onFailure { mensaje = it.message ?: "No se pudieron guardar los cambios." }
        }
    }

    Column(Modifier.fillMaxSize().background(FondoPrincipal)) {
        CabeceraPantalla(
            titulo = "Modo trabajador",
            subtitulo = "Ofrece tus servicios sin dejar de buscar empleo",
            onRegresar = onRegresar
        )

        if (cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MoradoPrincipal)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Estado actual (héroe con degradado cuando está activo) ──
                TarjetaEstadoTrabajador(activo = activo, disponible = disponible)

                TarjetaSeccion("Visibilidad", Icons.Default.Visibility, MoradoClaro) {
                    InterruptorCW(
                        titulo = "Ofrecer mis servicios",
                        descripcion = "Aparecerás en Servicios y en el mapa. Seguirás pudiendo buscar empleo y contratar.",
                        activo = activo
                    ) {
                        if (!guardando) {
                            activo = it
                            if (!it) disponible = false
                        }
                    }
                    HorizontalDivider(color = BordeSuave)
                    InterruptorCW(
                        titulo = "Disponible ahora",
                        descripcion = if (activo) "Si lo apagas, dejas de recibir solicitudes nuevas."
                        else "Activa primero \"Ofrecer mis servicios\".",
                        activo = disponible
                    ) {
                        if (activo && !guardando) disponible = it
                    }
                }

                if (activo) {
                    TarjetaSeccion("¿A qué te dedicas?", Icons.Default.Handyman, Menta) {
                        ChipsUnicos(
                            opciones = OficiosSugeridos,
                            seleccionado = OficiosSugeridos.firstOrNull { it.equals(oficio, ignoreCase = true) } ?: "",
                            permiteVacio = false
                        ) { oficio = it }
                        CampoCW(
                            label = "Oficio o servicio",
                            value = oficio,
                            maxChars = 60,
                            onChange = { oficio = it }
                        )
                        CampoCW(
                            label = "Describe tu servicio y experiencia",
                            value = descripcion,
                            minLines = 3,
                            maxChars = 500,
                            placeholder = "Ej. 8 años instalando y reparando tuberías en casas y negocios…",
                            onChange = { descripcion = it }
                        )
                    }

                    TarjetaSeccion("Tu ubicación", Icons.Default.LocationOn, AzulAcento) {
                        val tieneUbicacion = latitud != null && longitud != null
                        AvisoCW(
                            if (tieneUbicacion) {
                                "Ubicación guardada. Los clientes solo ven una zona aproximada (~1 km), nunca tu dirección."
                            } else {
                                "Aún no tienes ubicación. Sin ella no puedes aparecer como disponible."
                            },
                            if (tieneUbicacion) TipoAviso.EXITO else TipoAviso.INFO
                        )
                        BotonSecundarioCW(
                            texto = if (tieneUbicacion) "Actualizar con mi ubicación actual" else "Usar mi ubicación actual",
                            icono = Icons.Default.MyLocation,
                            onClick = {
                                if (tienePermiso(context)) {
                                    capturarUbicacion()
                                } else {
                                    permisoLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        )
                        if (ubicacionNueva) {
                            Text("Ubicación actualizada. Pulsa \"Guardar cambios\" para aplicarla.", color = Ambar, fontSize = 12.sp)
                        }
                    }
                }

                BotonCW(
                    texto = "Guardar cambios",
                    icono = Icons.Default.Check,
                    cargando = guardando,
                    onClick = { guardar() }
                )
            }
        }
    }

    if (mensaje.isNotBlank()) {
        DialogoCW(
            titulo = "Modo trabajador",
            onCerrar = { mensaje = "" },
            textoConfirmar = "Aceptar",
            textoCancelar = null,
            onConfirmar = {
                mensaje = ""
                if (cerrarAlAceptar) {
                    cerrarAlAceptar = false
                    onRegresar()
                }
            }
        ) {
            Text(mensaje, color = TextoSecundario, fontSize = 14.sp)
        }
    }
}

/** Resumen visual: degradado si el modo está activo, tarjeta tenue si no. */
@Composable
private fun TarjetaEstadoTrabajador(activo: Boolean, disponible: Boolean) {
    val fondo = if (activo) Brush.linearGradient(listOf(HeroInicio, HeroFin))
    else Brush.linearGradient(listOf(CardOscura, CardOscura))
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        border = if (activo) null else BorderStroke(1.dp, BordeSuave),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.background(fondo).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCaja(
                if (activo) Icons.Default.Handyman else Icons.Default.PersonOff,
                if (activo) Color.White else TextoSuave,
                48.dp
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (activo) "Modo trabajador activo" else "Modo trabajador apagado",
                    color = if (activo) Color.White else TextoPrincipal,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp
                )
                Text(
                    when {
                        !activo -> "Actívalo para recibir solicitudes de clientes cercanos."
                        disponible -> "Estás disponible: los clientes pueden pedirte servicios."
                        else -> "Estás oculto por ahora: nadie podrá pedirte servicios."
                    },
                    color = if (activo) Color.White.copy(alpha = .85f) else TextoSuave,
                    fontSize = 13.sp
                )
            }
            if (activo) Pastilla(if (disponible) "Disponible" else "Oculto", if (disponible) Exito else Ambar)
        }
    }
}

private fun tienePermiso(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

@SuppressLint("MissingPermission")
private fun ubicacionActual(context: Context): Location? {
    if (!tienePermiso(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val proveedores = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    return proveedores
        .mapNotNull { proveedor ->
            try {
                manager.getLastKnownLocation(proveedor)
            } catch (e: Exception) {
                null
            }
        }
        .maxByOrNull { it.time }
}
