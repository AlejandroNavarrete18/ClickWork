package com.example.fixnearv1.iuu.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TIPOS_CV = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
)
private const val MAX_CV_BYTES = 5L * 1024 * 1024

fun iconoMetodoPago(metodo: String): ImageVector = when (metodo) {
    "Efectivo" -> Icons.Default.Money
    "Transferencia" -> Icons.Default.SwapHoriz
    "Depósito" -> Icons.Default.CreditCard
    "Billetera digital" -> Icons.Default.PhoneAndroid
    "Cheque" -> Icons.Default.Receipt
    else -> Icons.Default.Payments
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetalleVacanteScreen(
    vacante: Vacante,
    onRegresar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mostrarFormulario by remember { mutableStateOf(false) }
    var aplicando by remember { mutableStateOf(false) }
    var tituloDialogo by remember { mutableStateOf("") }
    var mensajeDialogo by remember { mutableStateOf("") }

    // Formulario de postulación
    var mensaje by remember { mutableStateOf("") }
    var expectativa by remember { mutableStateOf("") }
    var cvUri by remember { mutableStateOf<Uri?>(null) }
    var cvNombre by remember { mutableStateOf("") }
    var cvMime by remember { mutableStateOf("application/pdf") }
    var errorFormulario by remember { mutableStateOf("") }

    val selectorCv = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            var nombre = "cv"
            var tam = 0L
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val iN = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val iS = c.getColumnIndex(OpenableColumns.SIZE)
                        if (iN >= 0) nombre = c.getString(iN) ?: "cv"
                        if (iS >= 0) tam = c.getLong(iS)
                    }
                }
            }
            if (tam > MAX_CV_BYTES) {
                errorFormulario = "El archivo pesa más de 5 MB. Elige uno más ligero."
            } else {
                cvUri = uri
                cvNombre = nombre
                cvMime = context.contentResolver.getType(uri) ?: "application/pdf"
                errorFormulario = ""
            }
        }
    }

    val pideCv = vacante.documentosRequeridos.any { it.equals("CV", ignoreCase = true) }

    fun enviarPostulacion() {
        val userId = SesionLocal.obtenerUserId(context)
        val token = SesionLocal.obtenerAccessToken(context)
        if (userId.isBlank() || token.isBlank()) {
            errorFormulario = "Inicia sesión nuevamente para aplicar."
            return
        }
        if (pideCv && vacante.permiteCv && cvUri == null) {
            errorFormulario = "Esta vacante pide tu CV. Adjunta un PDF o Word."
            return
        }
        errorFormulario = ""
        aplicando = true
        scope.launch {
            val perfil = SupabaseApi.obtenerPerfil(userId, token).getOrNull()
            if (perfil == null) {
                aplicando = false
                errorFormulario = "No se pudo obtener tu perfil."
                return@launch
            }

            var rutaCv = ""
            val uri = cvUri
            if (uri != null && vacante.permiteCv) {
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                }
                if (bytes == null) {
                    aplicando = false
                    errorFormulario = "No se pudo leer el archivo."
                    return@launch
                }
                val subida = SupabaseApi.subirCv(token, userId, cvNombre, bytes, cvMime)
                if (subida.isFailure) {
                    aplicando = false
                    errorFormulario = subida.exceptionOrNull()?.message ?: "No se pudo subir el CV."
                    return@launch
                }
                rutaCv = subida.getOrThrow()
            }

            val resultado = SupabaseApi.aplicarVacante(
                accessToken = token,
                vacante = vacante,
                perfil = perfil,
                mensaje = mensaje.trim(),
                expectativaSalarial = expectativa.trim(),
                cvPath = rutaCv,
                cvNombre = cvNombre
            )
            aplicando = false
            resultado.onSuccess {
                mostrarFormulario = false
                tituloDialogo = "¡Postulación enviada!"
                mensajeDialogo = "Aplicaste a ${vacante.puesto} en ${vacante.empresa}." +
                        if (rutaCv.isNotBlank()) " Tu CV se adjuntó de forma privada." else ""
            }.onFailure {
                errorFormulario = it.message ?: "Ocurrió un error al guardar la postulación."
            }
        }
    }

    val descripcionVacante = vacante.descripcion.takeIf { it.isNotBlank() }
        ?: "La empresa ${vacante.empresa} busca una persona responsable para cubrir el puesto de ${vacante.puesto}."
    val requisitosVacante = vacante.requisitos.takeIf { it.isNotBlank() }
        ?: "Disponibilidad en el horario indicado, responsabilidad y puntualidad."
    val beneficiosVacante = vacante.beneficios.takeIf { it.isNotBlank() }
        ?: "A convenir con el empleador."
    val direccionVacante = vacante.direccion.takeIf { it.isNotBlank() } ?: "Ubicación registrada"

    Scaffold(
        containerColor = FondoPrincipal,
        topBar = { CabeceraPantalla("Detalle de vacante", onRegresar = onRegresar) },
        bottomBar = {
            Surface(color = FondoSecundario, border = BorderStroke(1.dp, BordeSuave)) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val uri = if (vacante.latitud != null && vacante.longitud != null) {
                                Uri.parse("geo:${vacante.latitud},${vacante.longitud}?q=${vacante.latitud},${vacante.longitud}(${Uri.encode(vacante.empresa)})")
                            } else {
                                Uri.parse("geo:0,0?q=" + Uri.encode("${vacante.empresa} $direccionVacante"))
                            }
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                                .onFailure {
                                    tituloDialogo = "No se pudo abrir el mapa"
                                    mensajeDialogo = "No hay una aplicación de mapas disponible."
                                }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BordeSuave),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
                    ) {
                        Icon(Icons.Default.Map, null); Spacer(Modifier.width(8.dp)); Text("Mapa", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = { errorFormulario = ""; mostrarFormulario = true },
                        modifier = Modifier.weight(1.5f).height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Postularme", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Héroe ──
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(HeroInicio, HeroFin))).padding(20.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(56.dp).background(Color.White.copy(alpha = .2f), RoundedCornerShape(18.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(vacante.empresa.trim().firstOrNull()?.uppercase() ?: "?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(vacante.puesto, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                                Text(vacante.empresa, color = Color.White.copy(alpha = .88f), fontSize = 14.sp)
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (vacante.urgente) Pastilla("Urgente", Color.White, Icons.Default.Bolt)
                            if (vacante.tipoJornada.isNotBlank()) Pastilla(vacante.tipoJornada, Color.White)
                            if (vacante.modalidad.isNotBlank()) Pastilla(vacante.modalidad, Color.White)
                            if (vacante.categoria.isNotBlank()) Pastilla(vacante.categoria, Color.White)
                        }
                    }
                }
            }

            // ── Datos clave ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DatoClave(Modifier.weight(1.3f), Icons.Default.Payments, Exito, "Sueldo", vacante.salario.ifBlank { "A convenir" })
                DatoClave(Modifier.weight(1f), Icons.Default.Schedule, MoradoClaro, "Horario", vacante.horario.ifBlank { "A convenir" })
                DatoClave(Modifier.weight(0.8f), Icons.Default.Group, Ambar, "Plazas", "${vacante.plazas}")
            }

            // ── Formas de pago ──
            TarjetaSeccion("Pago", Icons.Default.AccountBalanceWallet, Exito) {
                if (vacante.periodoPago.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Se paga:", color = TextoSuave, fontSize = 13.sp)
                        Spacer(Modifier.width(8.dp))
                        Pastilla(vacante.periodoPago, Exito, Icons.Default.Event)
                    }
                }
                if (vacante.metodosPago.isNotEmpty()) {
                    Text("Formas de pago aceptadas", color = TextoSuave, fontSize = 13.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vacante.metodosPago.forEach { Pastilla(it, AzulAcento, iconoMetodoPago(it)) }
                    }
                } else if (vacante.periodoPago.isBlank()) {
                    Text("La forma de pago se acuerda directamente con el empleador.", color = TextoSecundario, fontSize = 13.sp)
                }
            }

            // ── Documentos ──
            TarjetaSeccion("Qué debes presentar", Icons.Default.FolderOpen, AzulAcento) {
                if (vacante.documentosRequeridos.isEmpty()) {
                    Text(
                        if (vacante.permiteCv) "No se piden documentos obligatorios. Puedes adjuntar tu CV para destacar." else "No se piden documentos para postularte.",
                        color = TextoSecundario, fontSize = 13.sp
                    )
                } else {
                    vacante.documentosRequeridos.forEach { doc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Exito, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(doc, color = TextoPrincipal, fontSize = 14.sp)
                        }
                    }
                }
                if (vacante.permiteCv) Pastilla("Puedes adjuntar tu CV (PDF o Word)", MoradoClaro, Icons.Default.AttachFile)
            }

            TarjetaSeccion("Descripción", Icons.Default.Work, MoradoClaro) {
                Text(descripcionVacante, color = TextoSecundario, fontSize = 14.sp, lineHeight = 21.sp)
            }
            TarjetaSeccion("Requisitos", Icons.Default.Checklist, Menta) {
                Text(requisitosVacante, color = TextoSecundario, fontSize = 14.sp, lineHeight = 21.sp)
            }
            TarjetaSeccion("Prestaciones", Icons.Default.CardGiftcard, Ambar) {
                Text(beneficiosVacante, color = TextoSecundario, fontSize = 14.sp, lineHeight = 21.sp)
            }

            TarjetaSeccion("Ubicación y contacto", Icons.Default.LocationOn, Coral) {
                Text(direccionVacante, color = TextoSecundario, fontSize = 14.sp)
                if (vacante.telefono.isNotBlank()) {
                    OutlinedButton(
                        onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${vacante.telefono}"))) } },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BordeSuave),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
                    ) {
                        Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(vacante.telefono)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // ── Hoja de postulación ──
    if (mostrarFormulario) {
        ModalBottomSheet(
            onDismissRequest = { if (!aplicando) mostrarFormulario = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CardOscura
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Postularme a ${vacante.puesto}", color = TextoPrincipal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(vacante.empresa, color = MoradoClaro, fontSize = 14.sp)

                CampoCW("Mensaje para el empleador (opcional)", mensaje, minLines = 3, maxChars = 500, placeholder = "Cuéntale por qué eres buena opción…") { mensaje = it }
                CampoCW("Sueldo que esperas (opcional)", expectativa, maxChars = 40, placeholder = "Ej. $9,500 quincenales") { expectativa = it }

                if (vacante.permiteCv) {
                    Text(if (pideCv) "Tu CV (obligatorio)" else "Tu CV (opcional)", color = TextoPrincipal, fontWeight = FontWeight.SemiBold)
                    if (cvUri == null) {
                        OutlinedButton(
                            onClick = { selectorCv.launch(TIPOS_CV) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MoradoPrincipal.copy(alpha = .6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MoradoClaro)
                        ) {
                            Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(10.dp)); Text("Adjuntar PDF o Word (máx. 5 MB)", fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(16.dp), color = CardOscura2, border = BorderStroke(1.dp, Exito.copy(alpha = .5f))) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconoCaja(Icons.Default.Description, Exito, 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(cvNombre, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Se guardará de forma privada", color = TextoSuave, fontSize = 12.sp)
                                }
                                IconButton(onClick = { cvUri = null; cvNombre = "" }) { Icon(Icons.Default.Close, "Quitar", tint = TextoSuave) }
                            }
                        }
                    }
                }

                Text(
                    "Tu nombre, correo y teléfono se comparten con el empleador al postularte.",
                    color = TextoSuave, fontSize = 12.sp
                )

                if (errorFormulario.isNotBlank()) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Peligro.copy(alpha = .12f), border = BorderStroke(1.dp, Peligro.copy(alpha = .4f))) {
                        Text(errorFormulario, Modifier.padding(12.dp), color = Peligro, fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = { enviarPostulacion() },
                    enabled = !aplicando,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal, disabledContainerColor = MoradoPrincipal.copy(alpha = .5f))
                ) {
                    if (aplicando) {
                        CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp)); Text("Enviando…", color = Color.White)
                    } else {
                        Icon(Icons.Default.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp)); Text("Enviar postulación", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (mensajeDialogo.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { mensajeDialogo = ""; tituloDialogo = "" },
            containerColor = CardOscura,
            shape = RoundedCornerShape(24.dp),
            title = { Text(tituloDialogo, color = TextoPrincipal, fontWeight = FontWeight.Bold) },
            text = { Text(mensajeDialogo, color = TextoSecundario) },
            confirmButton = {
                Button(
                    onClick = { mensajeDialogo = ""; tituloDialogo = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal)
                ) { Text("Aceptar", color = Color.White) }
            }
        )
    }
}

@Composable
private fun DatoClave(modifier: Modifier, icono: ImageVector, tint: Color, etiqueta: String, valor: String) {
    Surface(modifier = modifier, shape = RoundedCornerShape(18.dp), color = CardOscura, border = BorderStroke(1.dp, BordeSuave)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icono, null, tint = tint, modifier = Modifier.size(20.dp))
            Text(etiqueta, color = TextoSuave, fontSize = 11.sp)
            Text(valor, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
