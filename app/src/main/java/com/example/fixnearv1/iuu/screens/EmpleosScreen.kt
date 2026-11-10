package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import com.example.fixnearv1.utils.Vacante

private val FILTROS_BASE = listOf("Todas", "Urgentes", "Acepta CV", "Remoto", "Pago en efectivo")

@Composable
fun EmpleosScreen(
    onRegresar: () -> Unit,
    onVerDetalle: (Vacante) -> Unit
) {
    val context = LocalContext.current
    var busqueda by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf("Todas") }
    var verMapa by remember { mutableStateOf(true) }
    var vacantes by remember { mutableStateOf<List<Vacante>>(emptyList()) }
    var seleccionada by remember { mutableStateOf<Vacante?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val token = SesionLocal.obtenerAccessToken(context)
        if (token.isBlank()) {
            error = "No hay sesión activa. Inicia sesión nuevamente."
            cargando = false
            return@LaunchedEffect
        }
        SupabaseApi.obtenerVacantesDisponibles(accessToken = token)
            .onSuccess { vacantes = it }
            .onFailure { error = it.message ?: "No se pudieron cargar las vacantes." }
        cargando = false
    }

    val categorias = vacantes.map { it.categoria.trim() }.filter { it.isNotBlank() }.distinct().sorted()

    val filtradas = vacantes.filter { v ->
        val texto = "${v.empresa} ${v.puesto} ${v.direccion} ${v.categoria}"
        val coincideTexto = texto.contains(busqueda, ignoreCase = true)
        val coincideFiltro = when (filtro) {
            "Todas" -> true
            "Urgentes" -> v.urgente
            "Acepta CV" -> v.permiteCv
            "Remoto" -> v.modalidad.equals("Remoto", ignoreCase = true)
            "Pago en efectivo" -> v.metodosPago.any { it.equals("Efectivo", ignoreCase = true) }
            else -> v.categoria.equals(filtro, ignoreCase = true)
        }
        coincideTexto && coincideFiltro
    }

    Column(Modifier.fillMaxSize().background(FondoPrincipal)) {
        CabeceraPantalla(
            titulo = "Empleos",
            subtitulo = if (cargando) "Buscando oportunidades…" else "${filtradas.size} vacantes cerca de ti",
            onRegresar = onRegresar,
            acciones = {
                Surface(
                    onClick = { verMapa = !verMapa },
                    shape = RoundedCornerShape(50),
                    color = if (verMapa) MoradoPrincipal else CardOscura,
                    border = BorderStroke(1.dp, if (verMapa) MoradoPrincipal else BordeSuave)
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Mapa", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Puesto, negocio o zona…", color = TextoSuave) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MoradoClaro) },
                    trailingIcon = {
                        if (busqueda.isNotBlank()) IconButton(onClick = { busqueda = "" }) {
                            Icon(Icons.Default.Close, "Borrar", tint = TextoSuave)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardOscura, unfocusedContainerColor = CardOscura,
                        focusedBorderColor = MoradoPrincipal, unfocusedBorderColor = BordeSuave,
                        focusedTextColor = TextoPrincipal, unfocusedTextColor = TextoPrincipal,
                        cursorColor = MoradoPrincipal
                    )
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FILTROS_BASE + categorias) { f ->
                        val activo = filtro == f
                        Surface(
                            onClick = { filtro = f },
                            shape = RoundedCornerShape(50),
                            color = if (activo) MoradoPrincipal else CardOscura,
                            border = BorderStroke(1.dp, if (activo) MoradoPrincipal else BordeSuave)
                        ) {
                            Text(
                                f,
                                Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                color = if (activo) Color.White else TextoSecundario,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (verMapa) item {
                Surface(
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = CardOscura,
                    border = BorderStroke(1.dp, BordeSuave)
                ) {
                    VacantesMapInteractivo(
                        vacantes = filtradas,
                        vacanteSeleccionada = seleccionada,
                        onVacanteSeleccionada = { seleccionada = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            seleccionada?.let { v ->
                item { VacanteSeleccionadaCard(vacante = v, onVerVacante = { onVerDetalle(v) }) }
            }

            if (cargando) item {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MoradoPrincipal)
                }
            }

            if (error.isNotBlank()) item {
                Surface(shape = RoundedCornerShape(16.dp), color = Peligro.copy(alpha = .12f), border = BorderStroke(1.dp, Peligro.copy(alpha = .4f))) {
                    Text(error, Modifier.padding(14.dp), color = Peligro, fontSize = 13.sp)
                }
            }

            if (!cargando && error.isBlank() && filtradas.isEmpty()) item {
                EstadoVacio(
                    Icons.Default.SearchOff,
                    "Sin resultados",
                    "Prueba con otra palabra o quita los filtros para ver más vacantes."
                )
            }

            items(items = filtradas, key = { it.id }) { v ->
                VacanteTarjeta(
                    vacante = v,
                    resaltada = seleccionada?.id == v.id,
                    onClick = { onVerDetalle(v) }
                )
            }
        }
    }
}

@Composable
fun VacantesMapInteractivo(
    vacantes: List<Vacante>,
    vacanteSeleccionada: Vacante?,
    onVacanteSeleccionada: (Vacante) -> Unit,
    modifier: Modifier = Modifier
) {
    ClickWorkMiniMap(
        vacancies = vacantes,
        showJobs = true,
        showWorkers = false,
        onVacancySelected = onVacanteSeleccionada,
        modifier = modifier
    )
}

@Composable
fun VacanteSeleccionadaCard(vacante: Vacante, onVerVacante: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardOscura2,
        border = BorderStroke(1.dp, MoradoPrincipal.copy(alpha = .6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarInicial(vacante.empresa, 46.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Seleccionada en el mapa", color = AzulAcento, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(vacante.puesto, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${vacante.empresa} · ${vacante.salario.ifBlank { "Sueldo por acordar" }}", color = TextoSecundario, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onVerVacante,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) { Text("Ver", fontWeight = FontWeight.Bold) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VacanteTarjeta(
    vacante: Vacante,
    resaltada: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (resaltada) CardOscura2 else CardOscura),
        border = BorderStroke(1.dp, if (resaltada) MoradoPrincipal.copy(alpha = .7f) else BordeSuave),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarInicial(vacante.empresa, 46.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(vacante.puesto, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(vacante.empresa, color = MoradoClaro, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (vacante.urgente) Pastilla("Urgente", Ambar, Icons.Default.Bolt)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, null, tint = Exito, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    buildString {
                        append(vacante.salario.ifBlank { "Sueldo por acordar" })
                        if (vacante.periodoPago.isNotBlank()) append(" · ${vacante.periodoPago}")
                    },
                    color = Exito, fontWeight = FontWeight.Bold, fontSize = 15.sp
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (vacante.tipoJornada.isNotBlank()) Pastilla(vacante.tipoJornada, MoradoClaro, Icons.Default.Schedule)
                if (vacante.modalidad.isNotBlank()) Pastilla(vacante.modalidad, AzulAcento, Icons.Default.Place)
                if (vacante.categoria.isNotBlank()) Pastilla(vacante.categoria, Menta)
                if (vacante.plazas > 1) Pastilla("${vacante.plazas} plazas", Ambar, Icons.Default.Group)
                if (vacante.tipoJornada.isBlank() && vacante.horario.isNotBlank()) Pastilla(vacante.horario, MoradoClaro, Icons.Default.Schedule)
            }

            if (vacante.metodosPago.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = TextoSuave, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pago: " + vacante.metodosPago.joinToString(" · "), color = TextoSuave, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            HorizontalDivider(color = BordeSuave)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = TextoSuave, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(vacante.direccion.ifBlank { "Ubicación registrada" }, color = TextoSuave, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (vacante.permiteCv) Pastilla("Acepta CV", AzulAcento, Icons.Default.Description)
            }
        }
    }
}
