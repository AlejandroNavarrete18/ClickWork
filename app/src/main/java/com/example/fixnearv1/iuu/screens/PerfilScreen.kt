package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.PerfilUsuario
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SolicitudServicio
import com.example.fixnearv1.utils.SupabaseApi
import kotlinx.coroutines.launch

// ─── Tipos de diálogo de edición ────────────────────────────────────────────
private sealed class DialogoEdicion {
    object Ninguno : DialogoEdicion()
    object Nombre : DialogoEdicion()
    object Apellido : DialogoEdicion()
    object Correo : DialogoEdicion()
    object Password : DialogoEdicion()
    data class Info(val titulo: String, val mensaje: String) : DialogoEdicion()
}

@Composable
fun PerfilScreen(
    onRegresar: () -> Unit,
    onCerrarSesion: () -> Unit,
    onModoTrabajador: () -> Unit = {},
    onMisSolicitudes: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var perfil by remember { mutableStateOf<PerfilUsuario?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var errorCarga by remember { mutableStateOf("") }
    var solicitudes by remember { mutableStateOf<List<SolicitudServicio>?>(null) }

    // Estado del diálogo activo
    var dialogoActivo by remember { mutableStateOf<DialogoEdicion>(DialogoEdicion.Ninguno) }

    val userId = remember { SesionLocal.obtenerUserId(context) }

    // Carga inicial del perfil y del resumen de solicitudes (datos reales, sin cifras de ejemplo)
    LaunchedEffect(Unit) {
        val token = SesionLocal.obtenerAccessToken(context)

        if (userId.isBlank() || token.isBlank()) {
            errorCarga = "No hay sesión activa"
            cargando = false
            return@LaunchedEffect
        }

        val resultado = SupabaseApi.obtenerPerfil(userId = userId, accessToken = token)
        resultado.onSuccess { perfil = it }
        resultado.onFailure { errorCarga = it.message ?: "No se pudieron cargar tus datos" }
        cargando = false

        // Si falla, simplemente no se muestran las estadísticas
        SupabaseApi.obtenerMisSolicitudes(token, userId).onSuccess { solicitudes = it }
    }

    val nombreCompleto = when {
        cargando -> "Cargando..."
        perfil != null -> perfil!!.nombre.ifBlank { "Usuario" }
        else -> "Usuario"
    }

    val nombres = obtenerNombresPerfil(perfil?.nombre ?: "")
    val apellidos = obtenerApellidosPerfil(perfil?.nombre ?: "")
    val telefono = perfil?.telefono?.takeIf { it.isNotBlank() } ?: "Sin teléfono"
    val correo = perfil?.correo?.takeIf { it.isNotBlank() } ?: "Sin correo"
    val ofreceServicios = perfil?.ofreceServicios == true

    // ── Diálogos de edición ──────────────────────────────────────────────────
    when (val dialogo = dialogoActivo) {

        is DialogoEdicion.Nombre -> {
            DialogoEditarTexto(
                titulo = "Editar nombre",
                etiqueta = "Nombre(s)",
                valorInicial = nombres,
                onGuardar = { nuevoNombre ->
                    scope.launch {
                        val apellidoActual = obtenerApellidosPerfil(perfil?.nombre ?: "")
                        val nombreFinal = if (apellidoActual.isBlank())
                            nuevoNombre.trim()
                        else
                            "${nuevoNombre.trim()} $apellidoActual"

                        val token = SesionLocal.obtenerAccessToken(context)

                        val result = SupabaseApi.actualizarNombrePerfil(
                            userId = userId,
                            accessToken = token,
                            nuevoNombre = nombreFinal
                        )
                        result.onSuccess {
                            perfil = perfil?.copy(nombre = nombreFinal)
                            dialogoActivo = DialogoEdicion.Info(
                                "Nombre actualizado",
                                "Tu nombre se actualizó correctamente."
                            )
                        }
                        result.onFailure {
                            dialogoActivo = DialogoEdicion.Info(
                                "Error",
                                it.message ?: "No se pudo actualizar el nombre."
                            )
                        }
                    }
                },
                onDismiss = { dialogoActivo = DialogoEdicion.Ninguno }
            )
        }

        is DialogoEdicion.Apellido -> {
            DialogoEditarTexto(
                titulo = "Editar apellido",
                etiqueta = "Apellido(s)",
                valorInicial = apellidos,
                onGuardar = { nuevoApellido ->
                    scope.launch {
                        val nombreActual = obtenerNombresPerfil(perfil?.nombre ?: "")
                        val nombreFinal = if (nuevoApellido.isBlank())
                            nombreActual
                        else
                            "$nombreActual ${nuevoApellido.trim()}"

                        val token = SesionLocal.obtenerAccessToken(context)

                        val result = SupabaseApi.actualizarNombrePerfil(
                            userId = userId,
                            accessToken = token,
                            nuevoNombre = nombreFinal
                        )
                        result.onSuccess {
                            perfil = perfil?.copy(nombre = nombreFinal)
                            dialogoActivo = DialogoEdicion.Info(
                                "Apellido actualizado",
                                "Tu apellido se actualizó correctamente."
                            )
                        }
                        result.onFailure {
                            dialogoActivo = DialogoEdicion.Info(
                                "Error",
                                it.message ?: "No se pudo actualizar el apellido."
                            )
                        }
                    }
                },
                onDismiss = { dialogoActivo = DialogoEdicion.Ninguno }
            )
        }

        is DialogoEdicion.Correo -> {
            DialogoEditarTexto(
                titulo = "Cambiar correo electrónico",
                etiqueta = "Nuevo correo",
                valorInicial = perfil?.correo ?: "",
                teclado = KeyboardType.Email,
                onGuardar = { nuevoCorreo ->
                    scope.launch {
                        if (!nuevoCorreo.contains("@")) {
                            dialogoActivo = DialogoEdicion.Info(
                                "Correo inválido",
                                "Ingresa un correo electrónico válido."
                            )
                            return@launch
                        }
                        val token = SesionLocal.obtenerAccessToken(context)

                        val result = SupabaseApi.actualizarCorreo(
                            accessToken = token,
                            userId = userId,
                            nuevoCorreo = nuevoCorreo.trim()
                        )
                        result.onSuccess {
                            perfil = perfil?.copy(correo = nuevoCorreo.trim())
                            dialogoActivo = DialogoEdicion.Info(
                                "Correo actualizado",
                                "Se enviará un enlace de confirmación a $nuevoCorreo. " +
                                    "Confirma el cambio desde tu bandeja de entrada."
                            )
                        }
                        result.onFailure {
                            dialogoActivo = DialogoEdicion.Info(
                                "Error",
                                it.message ?: "No se pudo actualizar el correo."
                            )
                        }
                    }
                },
                onDismiss = { dialogoActivo = DialogoEdicion.Ninguno }
            )
        }

        is DialogoEdicion.Password -> {
            DialogoEditarPassword(
                onGuardar = { nuevaPassword ->
                    scope.launch {
                        if (nuevaPassword.length < 6) {
                            dialogoActivo = DialogoEdicion.Info(
                                "Contraseña muy corta",
                                "La contraseña debe tener al menos 6 caracteres."
                            )
                            return@launch
                        }
                        val token = SesionLocal.obtenerAccessToken(context)

                        val result = SupabaseApi.actualizarPassword(
                            accessToken = token,
                            nuevaPassword = nuevaPassword
                        )
                        result.onSuccess {
                            dialogoActivo = DialogoEdicion.Info(
                                "Contraseña actualizada",
                                "Tu contraseña se cambió correctamente."
                            )
                        }
                        result.onFailure {
                            dialogoActivo = DialogoEdicion.Info(
                                "Error",
                                it.message ?: "No se pudo cambiar la contraseña."
                            )
                        }
                    }
                },
                onDismiss = { dialogoActivo = DialogoEdicion.Ninguno }
            )
        }

        is DialogoEdicion.Info -> {
            DialogoCW(
                titulo = dialogo.titulo,
                onCerrar = { dialogoActivo = DialogoEdicion.Ninguno },
                textoConfirmar = "Aceptar",
                textoCancelar = null,
                onConfirmar = { dialogoActivo = DialogoEdicion.Ninguno }
            ) {
                Text(dialogo.mensaje, color = TextoSecundario, fontSize = 14.sp)
            }
        }

        is DialogoEdicion.Ninguno -> { /* nada */ }
    }

    // ── UI principal ─────────────────────────────────────────────────────────
    Column(Modifier.fillMaxSize().background(FondoPrincipal)) {
        CabeceraPantalla(
            titulo = "Mi perfil",
            subtitulo = "Tus datos y la seguridad de tu cuenta",
            onRegresar = onRegresar
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (errorCarga.isNotBlank()) item { AvisoCW(errorCarga, TipoAviso.ERROR) }

            // ── Héroe: avatar, nombre y correo ──
            item {
                Surface(shape = RoundedCornerShape(28.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
                    Box(Modifier.background(Brush.linearGradient(listOf(HeroInicio, HeroFin)))) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 40.dp, y = (-40).dp).size(140.dp)
                                .background(Color.White.copy(alpha = .08f), CircleShape)
                        )
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(shape = CircleShape, color = Color.White.copy(alpha = .25f)) {
                                Box(Modifier.padding(4.dp)) { AvatarInicial(nombreCompleto, 84.dp) }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                nombreCompleto,
                                color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                if (correo == "Sin correo") "Cuenta ClickWork" else correo,
                                color = Color.White.copy(alpha = .85f), fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                            if (ofreceServicios) {
                                Spacer(Modifier.height(4.dp))
                                Surface(color = Color.White.copy(alpha = .2f), shape = RoundedCornerShape(50)) {
                                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Handyman, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            perfil?.oficio?.takeIf { it.isNotBlank() } ?: "Ofrece servicios",
                                            color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Resumen de solicitudes (solo si se cargó) ──
            solicitudes?.let { lista ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniDato("${lista.count { it.clienteId == userId }}", "Pedidos", Modifier.weight(1f))
                        MiniDato("${lista.count { it.trabajadorId == userId }}", "Recibidos", Modifier.weight(1f))
                        MiniDato("${lista.count { it.estado == "Completada" }}", "Completados", Modifier.weight(1f))
                    }
                }
            }

            // ── Datos personales ──
            item {
                Column {
                EncabezadoGrupo("Datos personales")
                TarjetaAjustes {
                    FilaAjuste(Icons.Default.Person, "Nombre", valor = nombres.ifBlank { nombreCompleto }) {
                        dialogoActivo = DialogoEdicion.Nombre
                    }
                    DivisorCW()
                    FilaAjuste(Icons.Default.Badge, "Apellido", valor = apellidos.ifBlank { "Sin apellido" }) {
                        dialogoActivo = DialogoEdicion.Apellido
                    }
                    DivisorCW()
                    FilaAjuste(Icons.Default.Phone, "Teléfono", valor = telefono, tint = AzulAcento) {
                        dialogoActivo = DialogoEdicion.Info(
                            "Editar teléfono",
                            "Para cambiar tu número de teléfono contacta al soporte."
                        )
                    }
                    DivisorCW()
                    FilaAjuste(Icons.Default.Email, "Correo electrónico", valor = correo, tint = AzulAcento) {
                        dialogoActivo = DialogoEdicion.Correo
                    }
                }
                }
            }

            // ── Servicios ──
            item {
                Column {
                EncabezadoGrupo("Trabajo y servicios")
                TarjetaAjustes {
                    FilaAjuste(
                        Icons.Default.Handyman, "Modo trabajador",
                        valor = if (ofreceServicios) "Activo" else "Inactivo", tint = Menta
                    ) { onModoTrabajador() }
                    if (onMisSolicitudes != null) {
                        DivisorCW()
                        FilaAjuste(
                            Icons.Default.Assignment, "Mis solicitudes",
                            valor = solicitudes?.let { "${it.size}" } ?: "", tint = Ambar
                        ) { onMisSolicitudes() }
                    }
                }
                }
            }

            // ── Seguridad ──
            item {
                Column {
                EncabezadoGrupo("Seguridad")
                TarjetaAjustes {
                    FilaAjuste(Icons.Outlined.Lock, "Cambiar contraseña", tint = Exito) {
                        dialogoActivo = DialogoEdicion.Password
                    }
                    DivisorCW()
                    FilaAjuste(Icons.Default.Fingerprint, "Llave de acceso", pronto = true)
                    DivisorCW()
                    FilaAjuste(Icons.Default.VerifiedUser, "Verificar CURP", pronto = true)
                    DivisorCW()
                    FilaAjuste(Icons.Default.Share, "Mis redes sociales", pronto = true)
                    DivisorCW()
                    FilaAjuste(Icons.Default.Devices, "Mis dispositivos", pronto = true)
                }
                }
            }

            // ── Acciones de cuenta ──
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BotonSecundarioCW(
                        texto = "Cerrar sesión",
                        icono = Icons.Default.Logout,
                        color = TextoPrincipal,
                        onClick = {
                            SesionLocal.cerrarSesion(context)
                            onCerrarSesion()
                        }
                    )
                    BotonSecundarioCW(
                        texto = "Eliminar mi cuenta",
                        icono = Icons.Default.Delete,
                        color = Peligro,
                        onClick = {
                            dialogoActivo = DialogoEdicion.Info(
                                "Eliminar mi cuenta",
                                "Para eliminar tu cuenta de forma permanente contacta al soporte de ClickWork."
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EncabezadoGrupo(texto: String) {
    Text(
        texto.uppercase(),
        color = TextoSuave, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun MiniDato(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardOscura,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valor, color = TextoPrincipal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(etiqueta, color = TextoSuave, fontSize = 11.sp)
        }
    }
}

// ─── Diálogo genérico para editar texto ─────────────────────────────────────
@Composable
private fun DialogoEditarTexto(
    titulo: String,
    etiqueta: String,
    valorInicial: String,
    onGuardar: (String) -> Unit,
    onDismiss: () -> Unit,
    teclado: KeyboardType = KeyboardType.Text
) {
    var valor by remember { mutableStateOf(valorInicial) }
    var guardando by remember { mutableStateOf(false) }

    DialogoCW(
        titulo = titulo,
        onCerrar = onDismiss,
        textoConfirmar = "Guardar",
        confirmarHabilitado = valor.isNotBlank(),
        cargando = guardando,
        onConfirmar = {
            if (valor.isNotBlank() && !guardando) {
                guardando = true
                onGuardar(valor.trim())
            }
        }
    ) {
        CampoCW(
            label = etiqueta,
            value = valor,
            keyboardType = teclado,
            onChange = { valor = it.replace("\n", "") }
        )
    }
}

// ─── Diálogo para cambiar contraseña ────────────────────────────────────────
@Composable
private fun DialogoEditarPassword(
    onGuardar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nuevaPassword by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var errorLocal by remember { mutableStateOf("") }

    DialogoCW(
        titulo = "Cambiar contraseña",
        onCerrar = onDismiss,
        textoConfirmar = "Guardar",
        confirmarHabilitado = nuevaPassword.isNotBlank(),
        cargando = guardando,
        onConfirmar = {
            when {
                nuevaPassword.length < 6 -> errorLocal = "Mínimo 6 caracteres"
                nuevaPassword != confirmar -> errorLocal = "Las contraseñas no coinciden"
                else -> {
                    guardando = true
                    onGuardar(nuevaPassword)
                }
            }
        }
    ) {
        CampoIconoCW(
            value = nuevaPassword,
            onChange = { nuevaPassword = it; errorLocal = "" },
            placeholder = "Nueva contraseña",
            icono = Icons.Outlined.Lock,
            esPassword = true,
            habilitado = !guardando,
            contenedor = FondoPrincipal
        )
        CampoIconoCW(
            value = confirmar,
            onChange = { confirmar = it; errorLocal = "" },
            placeholder = "Confirmar contraseña",
            icono = Icons.Outlined.Lock,
            esPassword = true,
            imeAction = ImeAction.Done,
            habilitado = !guardando,
            contenedor = FondoPrincipal
        )
        if (errorLocal.isNotBlank()) AvisoCW(errorLocal, TipoAviso.ERROR)
    }
}

// ─── Helpers para nombre / apellido ─────────────────────────────────────────
fun obtenerNombresPerfil(nombreCompleto: String): String {
    val partes = nombreCompleto.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.isEmpty() -> ""
        partes.size >= 3 -> partes.take(2).joinToString(" ")
        else -> partes.first()
    }
}

fun obtenerApellidosPerfil(nombreCompleto: String): String {
    val partes = nombreCompleto.trim().split(" ").filter { it.isNotBlank() }
    return when {
        partes.size >= 3 -> partes.drop(2).joinToString(" ")
        partes.size == 2 -> partes.drop(1).joinToString(" ")
        else -> ""
    }
}
