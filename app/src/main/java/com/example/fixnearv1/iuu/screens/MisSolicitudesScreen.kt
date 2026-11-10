package com.example.fixnearv1.iuu.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.PagoInfo
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SolicitudServicio
import com.example.fixnearv1.utils.SupabaseApi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Pasos del recorrido normal de una solicitud (Cancelada/Rechazada salen de este flujo). */
private val PASOS_SOLICITUD = listOf("Pendiente", "Cotizada", "Confirmada", "En curso", "Completada")

/**
 * Seguimiento de solicitudes de servicio. Una misma persona puede verse aquí
 * como cliente (lo que pidió) y como trabajador (lo que le pidieron).
 * Todos los cambios de estado los valida Supabase; la app solo los solicita.
 */
@Composable
fun MisSolicitudesScreen(onRegresar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userId = remember { SesionLocal.obtenerUserId(context) }

    var solicitudes by remember { mutableStateOf<List<SolicitudServicio>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var ocupadoId by remember { mutableStateOf("") }
    var pestana by remember { mutableStateOf(0) }
    var cotizando by remember { mutableStateOf<SolicitudServicio?>(null) }
    var cancelando by remember { mutableStateOf<SolicitudServicio?>(null) }
    // id de solicitud -> estado del pago (solo lectura; lo cambia el webhook en el servidor)
    var pagos by remember { mutableStateOf<Map<String, PagoInfo>>(emptyMap()) }
    var eligiendoPago by remember { mutableStateOf<SolicitudServicio?>(null) }
    var esperandoRetornoPago by remember { mutableStateOf(false) }
    var chatDe by remember { mutableStateOf<SolicitudServicio?>(null) }

    fun recargar() {
        scope.launch {
            val token = SesionLocal.obtenerAccessToken(context)
            if (token.isBlank() || userId.isBlank()) {
                error = "No hay sesión activa."
                cargando = false
                return@launch
            }
            val resultado = SupabaseApi.obtenerMisSolicitudes(token, userId)
            resultado.onSuccess {
                solicitudes = it
                error = ""
            }
            resultado.onFailure {
                error = it.message ?: "No se pudieron cargar tus solicitudes."
            }
            // Si la tabla de pagos aún no existe o falla, la pantalla sigue funcionando sin pagos.
            SupabaseApi.obtenerMisPagos(token, userId).onSuccess { pagos = it }
            cargando = false
        }
    }

    // Ejecuta una acción del servidor, muestra el error si lo hay y recarga la lista.
    fun ejecutar(solicitud: SolicitudServicio, accion: suspend (String) -> Result<Unit>) {
        scope.launch {
            val token = SesionLocal.obtenerAccessToken(context)
            ocupadoId = solicitud.id
            val resultado = accion(token)
            resultado.onFailure {
                mensaje = it.message ?: "No se pudo completar la acción."
            }
            ocupadoId = ""
            recargar()
        }
    }

    // Pide el checkout al servidor y lo abre en el navegador (Mercado Pago).
    fun iniciarPago(solicitud: SolicitudServicio) {
        scope.launch {
            val token = SesionLocal.obtenerAccessToken(context)
            ocupadoId = solicitud.id
            SupabaseApi.crearPagoServicio(token, solicitud.id)
                .onSuccess { url ->
                    try {
                        esperandoRetornoPago = true
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: ActivityNotFoundException) {
                        esperandoRetornoPago = false
                        mensaje = "No hay un navegador para abrir el pago."
                    }
                }
                .onFailure { mensaje = it.message ?: "No se pudo iniciar el pago." }
            ocupadoId = ""
        }
    }

    LaunchedEffect(Unit) { recargar() }

    // Al volver de Mercado Pago se refresca el estado (el webhook puede tardar unos segundos).
    val ciclo = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(ciclo) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME && esperandoRetornoPago) {
                esperandoRetornoPago = false
                recargar()
            }
        }
        ciclo.addObserver(observador)
        onDispose { ciclo.removeObserver(observador) }
    }

    val comoCliente = solicitudes.filter { it.clienteId == userId }
    val comoTrabajador = solicitudes.filter { it.trabajadorId == userId }
    val lista = if (pestana == 0) comoCliente else comoTrabajador
    // Cuántas están esperando una acción mía (cotizar o confirmar)
    val porAtenderPedidos = comoCliente.count { it.estado == "Cotizada" }
    val porAtenderRecibidos = comoTrabajador.count { it.estado == "Pendiente" }

    chatDe?.let { c ->
        ChatSolicitudScreen(solicitud = c, onRegresar = { chatDe = null })
        return
    }

    Column(Modifier.fillMaxSize().background(FondoPrincipal)) {
        CabeceraPantalla(
            titulo = "Mis solicitudes",
            subtitulo = "Servicios que pediste y que te pidieron",
            onRegresar = onRegresar
        )

        Box(Modifier.padding(horizontal = 16.dp)) {
            SelectorPestana(
                pestana = pestana,
                pedidos = comoCliente.size,
                recibidos = comoTrabajador.size,
                atenderPedidos = porAtenderPedidos,
                atenderRecibidos = porAtenderRecibidos,
                onCambio = { pestana = it }
            )
        }

        if (cargando) {
            LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                color = MoradoPrincipal,
                trackColor = CardOscura
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (error.isNotBlank()) item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AvisoCW(error, TipoAviso.ERROR)
                    TextButton(onClick = { cargando = true; recargar() }) {
                        Text("Reintentar", color = MoradoClaro, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (!cargando && error.isBlank() && lista.isEmpty()) item {
                if (pestana == 0) {
                    EstadoVacio(
                        Icons.Default.Handyman,
                        "Aún no has pedido servicios",
                        "Búscalos en la pestaña Servicios y aquí verás cada paso: cotización, confirmación y cierre."
                    )
                } else {
                    EstadoVacio(
                        Icons.Default.Inbox,
                        "Nadie te ha solicitado todavía",
                        "Activa tu Modo trabajador desde Perfil para aparecer en Servicios y recibir solicitudes."
                    )
                }
            }

            items(lista, key = { it.id }) { s ->
                TarjetaSolicitud(
                    s = s,
                    userId = userId,
                    ocupado = ocupadoId == s.id,
                    pago = pagos[s.id],
                    onPagar = { eligiendoPago = s },
                    onResponderPago = { pagoId, recibido ->
                        ejecutar(s) { t -> SupabaseApi.responderPagoManual(t, pagoId, recibido) }
                    },
                    onCancelarAviso = { pagoId ->
                        ejecutar(s) { t -> SupabaseApi.cancelarPagoManual(t, pagoId) }
                    },
                    onChat = { chatDe = s },
                    onCotizar = { cotizando = s },
                    onRechazar = {
                        ejecutar(s) { t -> SupabaseApi.cambiarEstadoSolicitud(t, s.id, "Rechazada") }
                    },
                    onConfirmar = {
                        ejecutar(s) { t -> SupabaseApi.cambiarEstadoSolicitud(t, s.id, "Confirmada") }
                    },
                    onCancelar = { cancelando = s },
                    onIniciar = {
                        ejecutar(s) { t -> SupabaseApi.cambiarEstadoSolicitud(t, s.id, "En curso") }
                    },
                    onCompletar = {
                        ejecutar(s) { t -> SupabaseApi.cambiarEstadoSolicitud(t, s.id, "Completada") }
                    },
                    onReprogramar = {
                        elegirFechaHora(context) { iso ->
                            ejecutar(s) { t -> SupabaseApi.proponerFechaSolicitud(t, s.id, iso) }
                        }
                    },
                    onResponderFecha = { acepta ->
                        ejecutar(s) { t -> SupabaseApi.responderFechaSolicitud(t, s.id, acepta) }
                    }
                )
            }
        }
    }

    // ── Diálogo: cotizar ──
    cotizando?.let { s ->
        var precioTexto by remember(s.id) {
            mutableStateOf(
                s.precioCotizado?.let { String.format(Locale.US, "%.0f", it) } ?: ""
            )
        }
        DialogoCW(
            titulo = "Cotizar servicio",
            onCerrar = { cotizando = null },
            textoConfirmar = "Enviar cotización",
            textoCancelar = "Cerrar",
            onConfirmar = {
                val precio = precioTexto.toDoubleOrNull()
                if (precio == null || precio <= 0.0) {
                    mensaje = "Escribe un precio válido."
                } else {
                    cotizando = null
                    ejecutar(s) { t -> SupabaseApi.cotizarSolicitud(t, s.id, precio) }
                }
            }
        ) {
            Text(
                "Indica el precio total para \"${s.servicio}\". El cliente podrá confirmarlo o cancelar.",
                color = TextoSecundario, fontSize = 14.sp
            )
            CampoCW(
                label = "Precio en MXN",
                value = precioTexto,
                keyboardType = KeyboardType.Decimal,
                onChange = { nuevo -> precioTexto = nuevo.filter { c -> c.isDigit() || c == '.' }.take(10) }
            )
        }
    }

    // ── Diálogo: método de pago ──
    eligiendoPago?.let { sol ->
        val soyCli = sol.clienteId == userId
        val monto = sol.precioCotizado?.let { String.format(Locale.US, "%,.2f", it) } ?: "—"
        AlertDialog(
            onDismissRequest = { eligiendoPago = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = CardOscura,
            title = {
                Text(
                    if (soyCli) "¿Cómo quieres pagar?" else "¿Cómo te pagaron?",
                    color = TextoPrincipal, fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Total: $$monto MXN", color = Exito, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (soyCli) {
                        OpcionPago(
                            Icons.Default.CreditCard,
                            "Tarjeta, SPEI u OXXO",
                            "Crédito, débito, transferencia SPEI o efectivo en OXXO. Se paga en línea con Mercado Pago."
                        ) {
                            eligiendoPago = null
                            iniciarPago(sol)
                        }
                    }
                    OpcionPago(
                        Icons.Default.Payments,
                        "Efectivo",
                        if (soyCli) "Pagas en persona. Quedará como aviso hasta que el trabajador lo confirme."
                        else "El cliente te pagó en efectivo."
                    ) {
                        eligiendoPago = null
                        ejecutar(sol) { t -> SupabaseApi.registrarPagoManual(t, sol.id, "efectivo") }
                    }
                    OpcionPago(
                        Icons.Default.AccountBalance,
                        "Transferencia directa",
                        if (soyCli) "Transfieres al trabajador (pídele sus datos por el chat). Él deberá confirmar."
                        else "El cliente te transfirió directamente."
                    ) {
                        eligiendoPago = null
                        ejecutar(sol) { t -> SupabaseApi.registrarPagoManual(t, sol.id, "transferencia") }
                    }
                    Text(
                        "Efectivo y transferencia directa solo dejan constancia: ClickWork no mueve ni garantiza ese dinero.",
                        color = TextoSuave, fontSize = 11.sp
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { eligiendoPago = null }) { Text("Cancelar", color = TextoSecundario) }
            }
        )
    }

    // ── Diálogo: cancelar ──
    cancelando?.let { s ->
        var motivo by remember(s.id) { mutableStateOf("") }
        DialogoCW(
            titulo = "Cancelar solicitud",
            onCerrar = { cancelando = null },
            textoConfirmar = "Cancelar solicitud",
            textoCancelar = "Volver",
            colorConfirmar = Peligro,
            onConfirmar = {
                cancelando = null
                ejecutar(s) { t -> SupabaseApi.cambiarEstadoSolicitud(t, s.id, "Cancelada", motivo) }
            }
        ) {
            Text(
                "Esta acción no se puede deshacer. Puedes indicar el motivo (opcional).",
                color = TextoSecundario, fontSize = 14.sp
            )
            CampoCW(
                label = "Motivo",
                value = motivo,
                minLines = 2,
                maxChars = 300,
                onChange = { motivo = it }
            )
        }
    }

    // ── Mensajes de error / aviso ──
    if (mensaje.isNotBlank()) {
        DialogoCW(
            titulo = "Aviso",
            onCerrar = { mensaje = "" },
            textoConfirmar = "Aceptar",
            textoCancelar = null,
            onConfirmar = { mensaje = "" }
        ) {
            Text(mensaje, color = TextoSecundario, fontSize = 14.sp)
        }
    }
}

// ───────────────────────── Selector Pedidos / Recibidos ─────────────────────────

@Composable
private fun SelectorPestana(
    pestana: Int,
    pedidos: Int,
    recibidos: Int,
    atenderPedidos: Int,
    atenderRecibidos: Int,
    onCambio: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = CardOscura,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Row(Modifier.padding(4.dp)) {
            ItemPestana("Pedidos", pedidos, atenderPedidos, pestana == 0, Modifier.weight(1f)) { onCambio(0) }
            ItemPestana("Recibidos", recibidos, atenderRecibidos, pestana == 1, Modifier.weight(1f)) { onCambio(1) }
        }
    }
}

@Composable
private fun ItemPestana(
    texto: String,
    total: Int,
    atender: Int,
    activa: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = if (activa) MoradoPrincipal else Color.Transparent
    ) {
        Row(
            Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$texto ($total)",
                color = if (activa) Color.White else TextoSecundario,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            if (atender > 0) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(18.dp).background(Ambar, CircleShape), contentAlignment = Alignment.Center) {
                    Text("$atender", color = Color(0xFF3A2400), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ───────────────────────── Tarjeta de solicitud ─────────────────────────

@Composable
private fun TarjetaSolicitud(
    s: SolicitudServicio,
    userId: String,
    ocupado: Boolean,
    pago: PagoInfo?,
    onPagar: () -> Unit,
    onResponderPago: (String, Boolean) -> Unit,
    onCancelarAviso: (String) -> Unit,
    onChat: () -> Unit,
    onCotizar: () -> Unit,
    onRechazar: () -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    onIniciar: () -> Unit,
    onCompletar: () -> Unit,
    onReprogramar: () -> Unit,
    onResponderFecha: (Boolean) -> Unit
) {
    val soyCliente = s.clienteId == userId
    val contraparte = if (soyCliente) s.nombreTrabajador else s.nombreCliente
    val telefono = if (soyCliente) s.telefonoTrabajador else s.telefonoCliente
    val contactoVisible = s.estado in listOf("Confirmada", "En curso", "Completada")
    val abierta = s.estado in listOf("Pendiente", "Cotizada", "Confirmada")
    val colorEstado = colorDeEstado(s.estado)

    Surface(
        color = CardOscura,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BordeSuave),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Cabecera: avatar + servicio + estado
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarInicial(contraparte.ifBlank { s.servicio }, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        s.servicio,
                        color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        (if (soyCliente) "Trabajador: " else "Cliente: ") + contraparte.ifBlank { "—" },
                        color = TextoSuave, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Pastilla(s.estado, colorEstado)
            }

            // Recorrido de la solicitud
            if (s.estado in PASOS_SOLICITUD) PasosSolicitud(s.estado)

            if (s.descripcion.isNotBlank()) {
                Surface(shape = RoundedCornerShape(14.dp), color = CardOscura2, modifier = Modifier.fillMaxWidth()) {
                    Text(s.descripcion, Modifier.padding(12.dp), color = TextoSecundario, fontSize = 13.sp)
                }
            }

            if (s.precioCotizado != null) {
                DatoSolicitud(
                    Icons.Default.Payments, Exito,
                    "Precio cotizado: $" + String.format(Locale.US, "%,.2f", s.precioCotizado) + " MXN",
                    destacado = true
                )
            }
            if (s.fechaProgramada.isNotBlank()) {
                DatoSolicitud(Icons.Default.Event, MoradoClaro, "Fecha: " + formatearFecha(s.fechaProgramada))
            }
            if (contactoVisible && telefono.isNotBlank()) {
                DatoSolicitud(Icons.Default.Phone, AzulAcento, "Teléfono: $telefono")
            } else if (!contactoVisible && abierta) {
                Text(
                    "El teléfono se comparte cuando el cliente confirma.",
                    color = TextoSuave, fontSize = 11.sp
                )
            }
            if (s.estado == "Cancelada" && s.motivoCancelacion.isNotBlank()) {
                DatoSolicitud(Icons.Default.Info, Peligro, "Motivo: ${s.motivoCancelacion}")
            }

            // ── Chat con la otra persona (para aclarar la situación) ──
            if (s.estado !in listOf("Cancelada", "Rechazada")) {
                FilaAcciones {
                    BotonSol(
                        if (soyCliente) "Chatear con el trabajador" else "Chatear con el cliente",
                        Modifier.weight(1f), false, !ocupado, onChat
                    )
                }
            }

            // ── Pago ──
            // Los pagos en línea solo los marca "aprobado" el servidor (webhook de Mercado Pago).
            // Efectivo/transferencia son constancia entre las partes; la app no mueve dinero.
            val pagable = s.estado in listOf("Confirmada", "En curso", "Completada") &&
                    s.precioCotizado != null
            val estadoPago = pago?.estado.orEmpty()
            val nombreMetodo = when (pago?.metodo) {
                "efectivo" -> "efectivo"
                "transferencia" -> "transferencia"
                else -> "línea (Mercado Pago)"
            }
            when {
                estadoPago == "aprobado" || estadoPago == "confirmado" -> {
                    DatoSolicitud(
                        Icons.Default.CheckCircle, Exito,
                        (if (soyCliente) "Pagado en " else "Pago recibido en ") + nombreMetodo,
                        destacado = true
                    )
                }
                estadoPago == "en_revision" ->
                    AvisoCW("Estamos revisando este pago. Si necesitas ayuda, contacta a soporte.", TipoAviso.INFO)
                estadoPago == "reembolsado" ->
                    DatoSolicitud(Icons.Default.Info, AzulAcento, "Pago reembolsado")
                estadoPago == "por_confirmar" -> {
                    if (soyCliente) {
                        Nota("Avisaste que pagaste en $nombreMetodo. Esperando que el trabajador lo confirme.")
                        FilaAcciones {
                            BotonSol("Retirar aviso", Modifier.weight(1f), false, !ocupado, { onCancelarAviso(pago!!.id) })
                        }
                    } else {
                        AvisoCW("El cliente dice que pagó en $nombreMetodo. Confirma solo si ya lo recibiste.", TipoAviso.INFO)
                        FilaAcciones {
                            BotonSol("Sí, lo recibí", Modifier.weight(1f), true, !ocupado, { onResponderPago(pago!!.id, true) })
                            BotonSol("No lo recibí", Modifier.weight(1f), false, !ocupado, { onResponderPago(pago!!.id, false) }, peligro = true)
                        }
                    }
                }
                pagable -> {
                    if (estadoPago == "pendiente") {
                        Nota("Tienes un pago en línea en proceso. Si ya pagaste (p. ej. SPEI u OXXO), se confirmará al acreditarse.")
                    }
                    FilaAcciones {
                        BotonSol(
                            if (soyCliente) "Pagar" else "Registrar pago recibido",
                            Modifier.weight(1f), true, !ocupado, onPagar
                        )
                    }
                }
            }

            // ── Propuesta de nueva fecha ──
            if (abierta && s.fechaPropuesta.isNotBlank()) {
                if (s.propuestaPor == userId) {
                    AvisoCW("Propusiste ${formatearFecha(s.fechaPropuesta)}. Esperando respuesta.", TipoAviso.INFO)
                } else {
                    AvisoCW("Propone cambiar la fecha a ${formatearFecha(s.fechaPropuesta)}", TipoAviso.INFO)
                    FilaAcciones {
                        BotonSol("Aceptar fecha", Modifier.weight(1f), true, !ocupado, onClick = { onResponderFecha(true) })
                        BotonSol("Rechazar", Modifier.weight(1f), false, !ocupado, onClick = { onResponderFecha(false) })
                    }
                }
            }

            // ── Acciones según estado y rol ──
            when (s.estado) {
                "Pendiente" -> if (soyCliente) {
                    Nota("Esperando la cotización del trabajador.")
                } else {
                    FilaAcciones {
                        BotonSol("Cotizar", Modifier.weight(1f), true, !ocupado, onCotizar)
                        BotonSol("Rechazar", Modifier.weight(1f), false, !ocupado, onRechazar, peligro = true)
                    }
                }

                "Cotizada" -> if (soyCliente) {
                    FilaAcciones {
                        BotonSol("Confirmar", Modifier.weight(1f), true, !ocupado, onConfirmar)
                    }
                } else {
                    Nota("Esperando que el cliente confirme.")
                    FilaAcciones {
                        BotonSol("Cambiar precio", Modifier.weight(1f), false, !ocupado, onCotizar)
                        BotonSol("Rechazar", Modifier.weight(1f), false, !ocupado, onRechazar, peligro = true)
                    }
                }

                "Confirmada" -> if (!soyCliente) {
                    FilaAcciones {
                        BotonSol("Iniciar servicio", Modifier.weight(1f), true, !ocupado, onIniciar)
                    }
                } else {
                    Nota("Servicio confirmado.")
                }

                "En curso" -> if (soyCliente) {
                    FilaAcciones {
                        BotonSol("Marcar como completado", Modifier.weight(1f), true, !ocupado, onCompletar)
                    }
                } else {
                    Nota("Servicio en curso. El cliente lo cerrará al terminar.")
                }
            }

            if (abierta && s.fechaPropuesta.isBlank()) {
                FilaAcciones {
                    BotonSol("Reprogramar", Modifier.weight(1f), false, !ocupado, onReprogramar)
                    // El trabajador ya tiene "Rechazar" mientras no se confirma.
                    if (soyCliente || s.estado == "Confirmada") {
                        BotonSol("Cancelar", Modifier.weight(1f), false, !ocupado, onCancelar, peligro = true)
                    }
                }
            }

            if (ocupado) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = MoradoPrincipal, trackColor = CardOscura2)
            }
        }
    }
}

/** Línea de puntos: Pendiente → Cotizada → Confirmada → En curso → Completada. */
@Composable
private fun PasosSolicitud(estado: String) {
    val actual = PASOS_SOLICITUD.indexOf(estado)
    if (actual < 0) return
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        PASOS_SOLICITUD.forEachIndexed { i, nombre ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.weight(1f).height(2.dp).background(
                            when {
                                i == 0 -> Color.Transparent
                                i <= actual -> MoradoPrincipal
                                else -> BordeSuave
                            }
                        )
                    )
                    Box(
                        Modifier
                            .size(if (i == actual) 14.dp else 10.dp)
                            .background(if (i <= actual) MoradoPrincipal else BordeSuave, CircleShape)
                    )
                    Box(
                        Modifier.weight(1f).height(2.dp).background(
                            when {
                                i == PASOS_SOLICITUD.lastIndex -> Color.Transparent
                                i < actual -> MoradoPrincipal
                                else -> BordeSuave
                            }
                        )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    nombre,
                    color = if (i == actual) TextoPrincipal else TextoSuave,
                    fontSize = 9.sp,
                    fontWeight = if (i == actual) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DatoSolicitud(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    texto: String,
    destacado: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            texto,
            color = if (destacado) color else TextoSecundario,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (destacado) 14.sp else 13.sp
        )
    }
}

@Composable
private fun Nota(texto: String) {
    Text(texto, color = TextoSuave, fontSize = 12.sp)
}

@Composable
private fun FilaAcciones(contenido: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = contenido
    )
}

@Composable
private fun BotonSol(
    texto: String,
    modifier: Modifier,
    principal: Boolean,
    habilitado: Boolean,
    onClick: () -> Unit,
    peligro: Boolean = false
) {
    if (principal) {
        Button(
            onClick = onClick,
            enabled = habilitado,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal)
        ) { Text(texto, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1) }
    } else {
        val color = if (peligro) Peligro else MoradoClaro
        OutlinedButton(
            onClick = onClick,
            enabled = habilitado,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (peligro) Peligro.copy(alpha = .5f) else BordeSuave)
        ) { Text(texto, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
    }
}

@Composable
private fun OpcionPago(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = CardOscura2,
        border = BorderStroke(1.dp, BordeSuave),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = MoradoPrincipal, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(detalle, color = TextoSecundario, fontSize = 12.sp)
            }
        }
    }
}

private fun colorDeEstado(estado: String): Color = when (estado) {
    "Pendiente" -> Ambar
    "Cotizada" -> AzulAcento
    "Confirmada" -> Menta
    "En curso" -> MoradoClaro
    "Completada" -> Exito
    "Cancelada", "Rechazada" -> Coral
    else -> TextoSuave
}

/** Selector de fecha y hora con los diálogos nativos (funciona desde API 24). */
private fun elegirFechaHora(context: Context, alElegir: (String) -> Unit) {
    val ahora = Calendar.getInstance()
    val dialogoFecha = DatePickerDialog(
        context,
        { _, anio, mes, dia ->
            TimePickerDialog(
                context,
                { _, hora, minuto ->
                    val elegido = Calendar.getInstance().apply {
                        set(anio, mes, dia, hora, minuto, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    if (elegido.timeInMillis <= System.currentTimeMillis()) {
                        Toast.makeText(context, "Elige una fecha y hora futuras.", Toast.LENGTH_SHORT).show()
                    } else {
                        val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
                        alElegir(formato.format(elegido.time))
                    }
                },
                9, 0, false
            ).show()
        },
        ahora.get(Calendar.YEAR),
        ahora.get(Calendar.MONTH),
        ahora.get(Calendar.DAY_OF_MONTH)
    )
    dialogoFecha.datePicker.minDate = System.currentTimeMillis() - 1000
    dialogoFecha.show()
}

private fun formatearFecha(valor: String): String {
    if (valor.isBlank()) return ""
    return try {
        val entrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        val fecha = entrada.parse(valor) ?: return valor
        SimpleDateFormat("EEE d MMM, HH:mm", Locale("es", "MX")).format(fecha)
    } catch (e: Exception) {
        valor.take(16).replace('T', ' ')
    }
}
