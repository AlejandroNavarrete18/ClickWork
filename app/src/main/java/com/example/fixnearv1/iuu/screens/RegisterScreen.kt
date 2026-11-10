package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.R
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import com.example.fixnearv1.utils.Validaciones
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onCuentaCreada: () -> Unit,
    onRegresar: () -> Unit
) {
    // Estados de los campos
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // Estado para mostrar carga mientras registra
    var cargando by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Pistas en vivo de la contraseña (usan las mismas reglas que Validaciones.passwordValida)
    val tieneLargo = password.length >= 8
    val tieneLetra = password.any { it.isLetter() }
    val tieneNumero = password.any { it.isDigit() }
    val coinciden = password.isNotEmpty() && password == confirmar

    fun crearCuenta() {
        when {
            cargando -> return

            nombre.isBlank() ||
                    correo.isBlank() ||
                    telefono.isBlank() ||
                    password.isBlank() ||
                    confirmar.isBlank() -> {
                error = "Completa todos los campos"
            }

            password != confirmar -> {
                error = "Las contraseñas no coinciden"
            }

            !Validaciones.correoValido(correo) -> {
                error = "Escribe un correo válido"
            }

            !Validaciones.telefonoValido(telefono) -> {
                error = "El teléfono debe tener 10 dígitos"
            }

            !Validaciones.passwordValida(password) -> {
                error = "La contraseña debe tener mínimo 8 caracteres, con letras y números"
            }

            else -> {
                error = ""
                cargando = true

                scope.launch {
                    // Todas las cuentas nacen como usuario general: ofrecer servicios
                    // se activa después en Perfil → Modo trabajador.
                    val resultado = SupabaseApi.registrarUsuario(
                        nombre = nombre.trim(),
                        correo = correo.trim(),
                        telefono = Validaciones.normalizarTelefonoMx(telefono),
                        password = password,
                        tipoUsuario = "Cliente",
                        oficio = "",
                        descripcion = "",
                        disponible = false
                    )

                    resultado.onSuccess { sesion ->
                        SesionLocal.guardarSesion(
                            context = context,
                            sesion = sesion
                        )

                        cargando = false
                        onCuentaCreada()
                    }

                    resultado.onFailure { exception ->
                        cargando = false
                        error = exception.message ?: "Error al crear la cuenta"
                    }
                }
            }
        }
    }

    Column(Modifier.fillMaxSize().background(FondoPrincipal)) {
        CabeceraPantalla(
            titulo = "Crear cuenta",
            subtitulo = "Un solo perfil para buscar empleo, contratar y ofrecer servicios",
            onRegresar = { if (!cargando) onRegresar() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Logo y marca ---
            Image(
                painter = painterResource(id = R.drawable.ic_clickwork_logo),
                contentDescription = "Logo de ClickWork",
                modifier = Modifier.size(76.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = TextoPrincipal)) { append("Click") }
                    withStyle(SpanStyle(color = AzulAcento)) { append("Work") }
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(18.dp))

            // --- Formulario ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = CardOscura,
                border = BorderStroke(1.dp, BordeSuave)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CampoIconoCW(
                        value = nombre,
                        onChange = { nombre = it },
                        placeholder = "Nombre completo",
                        icono = Icons.Outlined.Person,
                        habilitado = !cargando
                    )
                    CampoIconoCW(
                        value = correo,
                        onChange = { correo = it },
                        placeholder = "Correo electrónico",
                        icono = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email,
                        habilitado = !cargando
                    )
                    CampoIconoCW(
                        value = telefono,
                        onChange = { telefono = it },
                        placeholder = "Teléfono (10 dígitos)",
                        icono = Icons.Outlined.Phone,
                        keyboardType = KeyboardType.Phone,
                        habilitado = !cargando
                    )
                    CampoIconoCW(
                        value = password,
                        onChange = { password = it },
                        placeholder = "Contraseña",
                        icono = Icons.Outlined.Lock,
                        esPassword = true,
                        habilitado = !cargando
                    )

                    // Requisitos de la contraseña, en vivo
                    if (password.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Requisito("Mínimo 8 caracteres", tieneLargo)
                            Requisito("Al menos una letra", tieneLetra)
                            Requisito("Al menos un número", tieneNumero)
                        }
                    }

                    CampoIconoCW(
                        value = confirmar,
                        onChange = { confirmar = it },
                        placeholder = "Confirmar contraseña",
                        icono = Icons.Outlined.Lock,
                        esPassword = true,
                        imeAction = ImeAction.Done,
                        habilitado = !cargando,
                        esError = confirmar.isNotEmpty() && !coinciden,
                        onHecho = { crearCuenta() }
                    )
                    if (confirmar.isNotEmpty()) Requisito("Las contraseñas coinciden", coinciden)

                    if (error.isNotEmpty()) AvisoCW(error, TipoAviso.ERROR)

                    BotonCW(
                        texto = "Crear cuenta",
                        icono = Icons.Default.ArrowForward,
                        cargando = cargando,
                        onClick = { crearCuenta() }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text("¿Ya tienes una cuenta? ", color = TextoSecundario, fontSize = 14.sp)
                Text(
                    text = "Iniciar sesión",
                    color = MoradoClaro,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !cargando) { onRegresar() }
                )
            }

            Spacer(Modifier.height(22.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            ) {
                Icon(Icons.Outlined.Lock, null, tint = MoradoClaro, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Tu información está segura", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextoPrincipal)
                    Text("Protegemos tus datos personales", fontSize = 11.sp, color = TextoSuave, textAlign = TextAlign.Start)
                }
            }
        }
    }
}

/** Línea de requisito: círculo vacío → check verde cuando se cumple. */
@Composable
private fun Requisito(texto: String, cumplido: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (cumplido) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (cumplido) Exito else TextoSuave,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(texto, color = if (cumplido) Exito else TextoSuave, fontSize = 12.sp)
    }
}
