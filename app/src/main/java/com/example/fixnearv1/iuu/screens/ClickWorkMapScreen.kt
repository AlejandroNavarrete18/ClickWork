package com.example.fixnearv1.iuu.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.PerfilUsuario
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import com.example.fixnearv1.utils.Vacante
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.maps.Style

private const val DARK_STYLE_URL = "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
private val CULIACAN = LatLng(24.8091, -107.3940)

private enum class MapFilter(val label: String) { ALL("Todo"), JOBS("Empleos"), WORKERS("Servicios") }

/**
 * Mapa de oportunidades. Es una pestaña del shell, por eso no lleva botón "atrás".
 * [onVerVacante] y [onVerTrabajador] se llaman desde la tarjeta del elemento seleccionado.
 */
@Composable
fun ClickWorkMapScreen(
    onVerVacante: (Vacante) -> Unit = {},
    onVerTrabajador: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var vacancies by remember { mutableStateOf<List<Vacante>>(emptyList()) }
    var workers by remember { mutableStateOf<List<PerfilUsuario>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var mapReady by remember { mutableStateOf(false) }
    var permissionMessage by remember { mutableStateOf("") }
    var selectedVacancy by remember { mutableStateOf<Vacante?>(null) }
    var selectedWorker by remember { mutableStateOf<PerfilUsuario?>(null) }
    var filter by remember { mutableStateOf(MapFilter.ALL) }
    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    // Relaciona cada marcador con su vacante o trabajador (por id del marcador, no por título,
    // para que dos vacantes con el mismo nombre no se confundan).
    val markerItems = remember { mutableMapOf<Long, Any>() }

    fun readLocation() {
        userLocation = readDeviceLocation(context) ?: CULIACAN
        mapLibreMap?.animateCamera(
            org.maplibre.android.camera.CameraUpdateFactory.newLatLngZoom(userLocation!!, 13.0)
        )
    }

    val locationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) {
            permissionMessage = ""
            readLocation()
        } else {
            permissionMessage = "Puedes explorar el mapa sin compartir tu ubicación."
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = hasLocationPermission(context)
        if (hasPermission) readLocation() else {
            userLocation = CULIACAN
            permissionMessage = "Vista inicial en Culiacán. Activa tu ubicación para ver oportunidades cercanas."
        }
    }

    LaunchedEffect(refresh) {
        loading = true
        val token = SesionLocal.obtenerAccessToken(context)
        if (token.isNotBlank()) {
            SupabaseApi.obtenerVacantesDisponibles(token).onSuccess { vacancies = it }
            SupabaseApi.obtenerTrabajadoresDisponibles(token).onSuccess { workers = it }
        }
        loading = false
    }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStart()
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    val ubicadasEmpleos = vacancies.count { it.latitud != null && it.longitud != null }
    val ubicadasServicios = workers.count { it.latitud != null && it.longitud != null }
    val totalUbicadas = when (filter) {
        MapFilter.JOBS -> ubicadasEmpleos
        MapFilter.WORKERS -> ubicadasServicios
        MapFilter.ALL -> ubicadasEmpleos + ubicadasServicios
    }

    Column(Modifier.fillMaxSize().background(FondoPrincipal)) {
        CabeceraPantalla(
            titulo = "Mapa",
            subtitulo = "Empleos y servicios cerca de ti",
            acciones = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = CardOscura,
                    border = BorderStroke(1.dp, BordeSuave)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(11.dp), color = MoradoClaro, strokeWidth = 2.dp)
                        } else {
                            Box(Modifier.size(8.dp).background(Exito, CircleShape))
                        }
                        Spacer(Modifier.width(7.dp))
                        Text(
                            if (loading) "Actualizando" else "$totalUbicadas ubicados",
                            color = TextoSecundario, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        )

        Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
            ChipsUnicos(
                opciones = MapFilter.values().map { it.label },
                seleccionado = filter.label,
                permiteVacio = false
            ) { etiqueta ->
                filter = MapFilter.values().first { it.label == etiqueta }
                selectedVacancy = null
                selectedWorker = null
            }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(26.dp),
                color = CardOscura,
                border = BorderStroke(1.dp, BordeSuave)
            ) {
                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = {
                            mapView.getMapAsync { map ->
                                mapLibreMap = map
                                map.uiSettings.isCompassEnabled = true
                                map.uiSettings.isLogoEnabled = false
                                map.uiSettings.isAttributionEnabled = true
                                map.cameraPosition = CameraPosition.Builder().target(userLocation ?: CULIACAN).zoom(12.2).build()
                                map.setStyle(Style.Builder().fromUri(DARK_STYLE_URL)) {
                                    mapReady = true
                                    drawMapMarkers(map, vacancies, workers, filter, markerItems)
                                }
                                map.setOnMarkerClickListener { marker ->
                                    val item = markerItems[marker.id]
                                    selectedVacancy = item as? Vacante
                                    selectedWorker = item as? PerfilUsuario
                                    true
                                }
                            }
                            mapView
                        },
                        update = { }
                    )

                    LaunchedEffect(mapLibreMap, vacancies, workers, filter, mapReady) {
                        val map = mapLibreMap
                        if (map != null && mapReady) drawMapMarkers(map, vacancies, workers, filter, markerItems)
                    }

                    if (!mapReady) {
                        Surface(
                            Modifier.align(Alignment.Center),
                            color = CardOscura2,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, BordeSuave)
                        ) {
                            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AzulAcento, modifier = Modifier.size(25.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.height(10.dp))
                                Text("Preparando tu mapa", color = TextoPrincipal, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Conectando con oportunidades cercanas", color = TextoSuave, fontSize = 11.sp)
                            }
                        }
                    }
                    if (loading) LinearProgressIndicator(
                        Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                        color = MoradoPrincipal,
                        trackColor = Color.Transparent
                    )

                    // Botones flotantes (suben cuando hay una tarjeta seleccionada)
                    val haySeleccion = selectedVacancy != null || selectedWorker != null
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(12.dp).padding(bottom = if (haySeleccion) 118.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = {
                                if (hasLocationPermission(context)) readLocation()
                                else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            containerColor = CardOscura2,
                            contentColor = AzulAcento
                        ) { Icon(Icons.Default.MyLocation, "Centrar en mi ubicación") }
                        SmallFloatingActionButton(
                            onClick = { refresh++ },
                            containerColor = CardOscura2,
                            contentColor = MoradoClaro
                        ) { Icon(Icons.Default.Refresh, "Actualizar oportunidades") }
                    }

                    // Tarjeta del elemento seleccionado, sobre el mapa
                    selectedVacancy?.let { vacancy ->
                        PanelSeleccion(
                            icono = Icons.Default.Work,
                            tint = MoradoClaro,
                            titulo = vacancy.puesto,
                            subtitulo = vacancy.empresa,
                            detalle = buildString {
                                append(vacancy.salario.ifBlank { "Sueldo por acordar" })
                                if (vacancy.periodoPago.isNotBlank()) append(" · ${vacancy.periodoPago}")
                                if (vacancy.horario.isNotBlank()) append(" · ${vacancy.horario}")
                            },
                            detalle2 = vacancy.direccion,
                            textoBoton = "Ver vacante",
                            onBoton = { onVerVacante(vacancy) },
                            onCerrar = { selectedVacancy = null },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                    selectedWorker?.let { worker ->
                        PanelSeleccion(
                            icono = Icons.Default.Handyman,
                            tint = AzulAcento,
                            titulo = worker.nombre.ifBlank { worker.oficio.ifBlank { "Trabajador" } },
                            subtitulo = worker.oficio.ifBlank { "Trabajador disponible" },
                            detalle = worker.descripcion,
                            detalle2 = "",
                            textoBoton = "Ver perfil",
                            onBoton = { onVerTrabajador(worker.id) },
                            onCerrar = { selectedWorker = null },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }

        // Atribución obligatoria de los mapas (OpenStreetMap / CARTO) y avisos
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (permissionMessage.isNotBlank()) permissionMessage else "Toca un punto para ver el detalle",
                color = TextoSuave, fontSize = 10.sp,
                modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(8.dp))
            Text("© OpenStreetMap · CARTO", color = TextoSuave, fontSize = 9.sp)
        }
        // Espacio para que la barra inferior no tape el mapa
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun PanelSeleccion(
    icono: ImageVector,
    tint: Color,
    titulo: String,
    subtitulo: String,
    detalle: String,
    detalle2: String,
    textoBoton: String,
    onBoton: () -> Unit,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(10.dp),
        color = CardOscura2,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MoradoPrincipal.copy(alpha = .6f))
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCaja(icono, tint, 42.dp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(titulo, color = TextoPrincipal, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitulo, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onCerrar) { Icon(Icons.Default.Close, "Cerrar", tint = TextoSecundario) }
            }
            if (detalle.isNotBlank()) {
                Text(detalle, color = TextoSecundario, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (detalle2.isNotBlank()) {
                Text(detalle2, color = TextoSuave, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = onBoton,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal)
            ) { Text(textoBoton, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}

private fun drawMapMarkers(
    map: MapLibreMap,
    vacancies: List<Vacante>,
    workers: List<PerfilUsuario>,
    filter: MapFilter,
    markerItems: MutableMap<Long, Any>
) {
    map.clear()
    markerItems.clear()
    if (filter != MapFilter.WORKERS) {
        vacancies.filter { it.latitud != null && it.longitud != null }.forEach { vacancy ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(vacancy.latitud!!, vacancy.longitud!!))
                    .title(vacancy.puesto)
                    .snippet("EMPLEO · ${vacancy.empresa} · ${vacancy.salario}")
            )
            markerItems[marker.id] = vacancy
        }
    }
    if (filter != MapFilter.JOBS) {
        workers.filter { it.latitud != null && it.longitud != null }.forEach { worker ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(worker.latitud!!, worker.longitud!!))
                    .title(worker.nombre.ifBlank { worker.oficio })
                    .snippet("SERVICIO · ${worker.oficio}")
            )
            markerItems[marker.id] = worker
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Suppress("MissingPermission")
private fun readDeviceLocation(context: Context): LatLng? {
    if (!hasLocationPermission(context)) return null
    return try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best: Location? = manager.getProviders(true)
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        best?.let { LatLng(it.latitude, it.longitude) }
    } catch (_: Exception) {
        null
    }
}
