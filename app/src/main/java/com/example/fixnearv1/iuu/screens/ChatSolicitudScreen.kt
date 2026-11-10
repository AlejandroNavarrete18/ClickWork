package com.example.fixnearv1.iuu.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.MensajeChat
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SolicitudServicio
import com.example.fixnearv1.utils.SupabaseApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Chat entre cliente y trabajador de UNA solicitud, para aclarar la situación
 * (qué hay que hacer, fotos descritas, horarios...) antes o durante el servicio.
 * Solo los dos participantes pueden leerlo (RLS). Se actualiza cada pocos segundos.
 */
@Composable
fun ChatSolicitudScreen(solicitud: SolicitudServicio, onRegresar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember { SesionLocal.obtenerUserId(context) }
    val soyCliente = solicitud.clienteId == userId
    val contraparte = (if (soyCliente) solicitud.nombreTrabajador else solicitud.nombreCliente)
        .ifBlank { if (soyCliente) "Trabajador" else "Cliente" }

    var conversacionId by remember { mutableStateOf("") }
    var mensajes by remember { mutableStateOf<List<MensajeChat>>(emptyList()) }
    var texto by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var enviando by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    BackHandler(onBack = onRegresar)

    // 1. Abrir (o crear) la conversación de esta solicitud
    LaunchedEffect(solicitud.id) {
        val token = SesionLocal.obtenerAccessToken(context)
        SupabaseApi.asegurarConversacion(token, solicitud.id)
            .onSuccess { conversacionId = it; error = "" }
            .onFailure { error = it.message ?: "No se pudo abrir el chat." }
        cargando = false
    }

    // 2. Actualizar mensajes cada 4 s mientras la pantalla esté abierta
    LaunchedEffect(conversacionId) {
        if (conversacionId.isBlank()) return@LaunchedEffect
        while (true) {
            val token = SesionLocal.obtenerAccessToken(context)
            SupabaseApi.obtenerMensajes(token, conversacionId).onSuccess { nuevos ->
                if (nuevos != mensajes) mensajes = nuevos
                if (nuevos.any { it.remitenteId != userId && !it.leido }) {
                    SupabaseApi.marcarMensajesLeidos(token, conversacionId)
                }
            }
            delay(4000)
        }
    }

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) listState.animateScrollToItem(mensajes.lastIndex)
    }

    fun enviar() {
        val t = texto.trim()
        if (t.isEmpty() || conversacionId.isBlank() || enviando) return
        scope.launch {
            enviando = true
            val token = SesionLocal.obtenerAccessToken(context)
            SupabaseApi.enviarMensaje(token, conversacionId, userId, t)
                .onSuccess {
                    texto = ""
                    error = ""
                    SupabaseApi.obtenerMensajes(token, conversacionId).onSuccess { mensajes = it }
                }
                .onFailure { error = it.message ?: "No se pudo enviar el mensaje." }
            enviando = false
        }
    }

    Column(Modifier.fillMaxSize().background(FondoPrincipal).imePadding()) {
        CabeceraPantalla(
            titulo = contraparte,
            subtitulo = solicitud.servicio,
            onRegresar = onRegresar
        )

        if (cargando) {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = MoradoPrincipal, trackColor = CardOscura2)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (!cargando && error.isBlank() && mensajes.isEmpty()) {
                EstadoVacio(
                    Icons.Default.Forum,
                    "Aún no hay mensajes",
                    if (soyCliente) "Cuéntale al trabajador los detalles: qué necesitas, dónde y cuándo."
                    else "Pregunta lo que necesites saber para cotizar y hacer bien el trabajo."
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mensajes, key = { it.id }) { m ->
                    BurbujaMensaje(m, propio = m.remitenteId == userId)
                }
            }
        }

        if (error.isNotBlank()) {
            Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) { AvisoCW(error, TipoAviso.ERROR) }
        }

        Surface(color = FondoSecundario, border = BorderStroke(1.dp, BordeSuave)) {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(3000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje", color = TextoSuave) },
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = FondoSecundario, unfocusedContainerColor = FondoSecundario,
                        focusedTextColor = TextoPrincipal, unfocusedTextColor = TextoPrincipal,
                        focusedBorderColor = MoradoPrincipal, unfocusedBorderColor = BordeSuave,
                        cursorColor = MoradoClaro
                    )
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = { enviar() },
                    enabled = texto.isNotBlank() && conversacionId.isNotBlank() && !enviando,
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MoradoPrincipal)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(m: MensajeChat, propio: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (propio) Arrangement.End else Arrangement.Start) {
        Surface(
            color = if (propio) MoradoPrincipal else CardOscura,
            shape = RoundedCornerShape(
                topStart = 18.dp, topEnd = 18.dp,
                bottomStart = if (propio) 18.dp else 4.dp,
                bottomEnd = if (propio) 4.dp else 18.dp
            ),
            border = if (propio) null else BorderStroke(1.dp, BordeSuave),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(m.contenido, color = if (propio) Color.White else TextoPrincipal, fontSize = 15.sp)
                Text(
                    horaCorta(m.creadoEn) + if (propio && m.leido) "  ✓✓" else "",
                    color = if (propio) Color.White.copy(alpha = .75f) else TextoSuave,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

private fun horaCorta(iso: String): String {
    return try {
        val limpio = iso.replace("T", " ").take(19)
        val entrada = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        SimpleDateFormat("d MMM HH:mm", Locale("es", "MX")).format(entrada.parse(limpio)!!)
    } catch (e: Exception) {
        ""
    }
}
