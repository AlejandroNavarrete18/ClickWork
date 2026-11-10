package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fixnearv1.utils.PerfilUsuario
import com.example.fixnearv1.utils.Vacante
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val CLICKWORK_DARK_STYLE = "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
private val CLICKWORK_DEFAULT_CENTER = LatLng(24.8091, -107.3940)

/** Mapa compacto compartido por Empleos y Servicios: mismo estilo, motor y comportamiento que Live Map. */
@Composable
fun ClickWorkMiniMap(
    vacancies: List<Vacante> = emptyList(),
    workers: List<PerfilUsuario> = emptyList(),
    showJobs: Boolean,
    showWorkers: Boolean,
    onVacancySelected: (Vacante) -> Unit = {},
    onWorkerSelected: (PerfilUsuario) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }
    val mapRef = remember { mutableStateOf<MapLibreMap?>(null) }

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

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            mapView.getMapAsync { map ->
                mapRef.value = map
                map.uiSettings.isCompassEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.cameraPosition = CameraPosition.Builder()
                    .target(CLICKWORK_DEFAULT_CENTER)
                    .zoom(12.4)
                    .build()
                map.setStyle(Style.Builder().fromUri(CLICKWORK_DARK_STYLE)) {
                    renderClickWorkMiniMarkers(context, map, vacancies, workers, showJobs, showWorkers)
                    map.setOnMarkerClickListener { marker ->
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 14.8))
                        dispatchMiniMarker(marker, vacancies, workers, onVacancySelected, onWorkerSelected)
                        true
                    }
                }
            }
            mapView
        },
        update = {
            mapRef.value?.let { map ->
                renderClickWorkMiniMarkers(context, map, vacancies, workers, showJobs, showWorkers)
                map.setOnMarkerClickListener { marker ->
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 14.8))
                    dispatchMiniMarker(marker, vacancies, workers, onVacancySelected, onWorkerSelected)
                    true
                }
            }
        }
    )
}

private fun renderClickWorkMiniMarkers(
    context: android.content.Context,
    map: MapLibreMap,
    vacancies: List<Vacante>,
    workers: List<PerfilUsuario>,
    showJobs: Boolean,
    showWorkers: Boolean
) {
    map.clear()
    // MapLibre 11 ya no permite defaultMarker(tono): se dibujan marcadores de color propio.
    val jobIcon = coloredMarkerIcon(context, 0xFF22D3EE.toInt())
    val workerIcon = coloredMarkerIcon(context, 0xFF6D5BFF.toInt())
    if (showJobs) vacancies.forEach { vacancy ->
        val lat = vacancy.latitud ?: return@forEach
        val lon = vacancy.longitud ?: return@forEach
        map.addMarker(
            MarkerOptions().position(LatLng(lat, lon))
                .icon(jobIcon)
                .title("job:${vacancy.id}")
                .snippet("EMPLEO · ${vacancy.puesto} · ${vacancy.empresa}")
        )
    }
    if (showWorkers) workers.forEach { worker ->
        val lat = worker.latitud ?: return@forEach
        val lon = worker.longitud ?: return@forEach
        map.addMarker(
            MarkerOptions().position(LatLng(lat, lon))
                .icon(workerIcon)
                .title("worker:${worker.id}")
                .snippet("SERVICIO · ${worker.oficio.ifBlank { "Profesional" }} · ${worker.nombre}")
        )
    }
}

private fun dispatchMiniMarker(
    marker: Marker,
    vacancies: List<Vacante>,
    workers: List<PerfilUsuario>,
    onVacancySelected: (Vacante) -> Unit,
    onWorkerSelected: (PerfilUsuario) -> Unit
) {
    val key = marker.title.orEmpty()
    when {
        key.startsWith("job:") -> vacancies.firstOrNull { it.id == key.removePrefix("job:") }?.let(onVacancySelected)
        key.startsWith("worker:") -> workers.firstOrNull { it.id == key.removePrefix("worker:") }?.let(onWorkerSelected)
    }
}

/** Círculo de color con borde blanco, usado como marcador del mapa. */
private fun coloredMarkerIcon(context: android.content.Context, color: Int): Icon {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = 0xFFFFFFFF.toInt()
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8f, paint)
    return IconFactory.getInstance(context).fromBitmap(bitmap)
}
