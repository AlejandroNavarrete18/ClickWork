package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.*

@Composable
fun ClickWorkHomeScreen(
    onJobs: () -> Unit,
    onServices: () -> Unit,
    onMap: () -> Unit,
    onCompany: () -> Unit,
    onQr: () -> Unit,
    onRequests: () -> Unit = {},
    onWorkerMode: () -> Unit = {}
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("Tu espacio profesional") }
    var vacantes by remember { mutableStateOf<List<Vacante>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = SesionLocal.obtenerUserId(context)
        val token = SesionLocal.obtenerAccessToken(context)
        if (uid.isNotBlank() && token.isNotBlank()) {
            SupabaseApi.obtenerPerfil(uid, token).onSuccess { nombre = it.nombre.substringBefore(' ') }
            SupabaseApi.obtenerVacantesDisponibles(token).onSuccess { vacantes = it.take(3) }
        }
        loading = false
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FondoPrincipal),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Encabezado ──
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CLICKWORK", color = MoradoClaro, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(Modifier.height(2.dp))
                    Text("Hola, $nombre", color = TextoPrincipal, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Tu siguiente oportunidad empieza aquí.", color = TextoSuave, fontSize = 13.sp)
                }
                Spacer(Modifier.width(12.dp))
                Surface(shape = RoundedCornerShape(16.dp), color = CardOscura, border = BorderStroke(1.dp, BordeSuave)) {
                    IconButton(onClick = onQr) { Icon(Icons.Default.QrCodeScanner, "Pasaporte QR", tint = MoradoClaro) }
                }
            }
        }

        // ── Héroe ──
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.background(Brush.linearGradient(listOf(HeroInicio, HeroFin)))) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 50.dp, y = (-50).dp).size(170.dp).background(Color.White.copy(alpha = .08f), CircleShape))
                    Box(Modifier.align(Alignment.BottomEnd).offset(x = (-30).dp, y = 60.dp).size(120.dp).background(Color.White.copy(alpha = .06f), CircleShape))
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = Color.White.copy(alpha = .18f), shape = RoundedCornerShape(50)) {
                            Text("EMPLEO + SERVICIOS", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        }
                        Text("Conecta con nuevas oportunidades", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 29.sp)
                        Text("Encuentra trabajo, publica vacantes o contrata talento cerca de ti.", color = Color.White.copy(alpha = .88f), fontSize = 13.sp, lineHeight = 19.sp)
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onJobs,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1A1250)),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text("Empleos", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.width(6.dp)); Icon(Icons.Default.ArrowForward, null, Modifier.size(18.dp))
                            }
                            OutlinedButton(
                                onClick = onServices,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = .55f)),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Buscar servicio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Acciones rápidas ──
        item {
            Text("¿Qué necesitas hoy?", color = TextoPrincipal, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Modifier.weight(1f), Icons.Default.Work, MoradoClaro, "Buscar empleo", "Vacantes y empresas", onJobs)
                QuickAction(Modifier.weight(1f), Icons.Default.Search, AzulAcento, "Buscar servicio", "Contrata profesionales", onServices)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Modifier.weight(1f), Icons.Default.Map, Menta, "Explorar mapa", "Oportunidades por zona", onMap)
                QuickAction(Modifier.weight(1f), Icons.Default.Business, Ambar, "Publicar vacante", "Como particular o empresa", onCompany)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickAction(Modifier.weight(1f), Icons.Default.Checklist, Coral, "Mis solicitudes", "Pedidos y recibidos", onRequests)
                QuickAction(Modifier.weight(1f), Icons.Default.Engineering, MoradoClaro, "Ofrecer servicios", "Activa tu modo trabajador", onWorkerMode)
            }
        }

        // ── Oportunidades recientes ──
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Oportunidades recientes", color = TextoPrincipal, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = onJobs) { Text("Ver todas", color = MoradoClaro, fontWeight = FontWeight.SemiBold) }
            }
        }
        if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MoradoPrincipal, trackColor = CardOscura2) }
        else if (vacantes.isEmpty()) item {
            Surface(color = CardOscura, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, BordeSuave), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconChip(Icons.Default.Explore, MoradoClaro)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Tu próxima oportunidad", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                        Text("Explora las vacantes publicadas en ClickWork.", color = TextoSuave, fontSize = 12.sp)
                    }
                }
            }
        } else items(vacantes.size) { index ->
            val v = vacantes[index]
            Surface(color = CardOscura, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, BordeSuave), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    IconChip(Icons.Default.Work, MoradoClaro)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(v.puesto, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(v.empresa, color = MoradoClaro, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, null, tint = Exito, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
                            Text(v.salario.ifBlank { "Sueldo por acordar" }, color = Exito, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = TextoSuave, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp))
                            Text(v.direccion, color = TextoSuave, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        // ── Reputación ──
        item {
            Surface(
                color = Color.Transparent,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AzulAcento.copy(alpha = .35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.background(Brush.horizontalGradient(listOf(AzulAcento.copy(alpha = .14f), MoradoPrincipal.copy(alpha = .10f)))).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconChip(Icons.Default.VerifiedUser, AzulAcento)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Tu reputación también cuenta", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                        Text("Construye un historial verificable con tu pasaporte ClickWork.", color = TextoSecundario, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun IconChip(icon: ImageVector, tint: Color) {
    Box(Modifier.size(42.dp).background(tint.copy(alpha = .16f), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun QuickAction(modifier: Modifier, icon: ImageVector, tint: Color, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(122.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardOscura),
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            IconChip(icon, tint)
            Column {
                Text(title, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = TextoSuave, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
