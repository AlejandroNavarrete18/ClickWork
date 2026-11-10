package com.example.fixnearv1.iuu.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.R
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginExitoso: () -> Unit,
    onCrearCuenta: () -> Unit,
    onOlvidoPassword: () -> Unit
) {
    // Estados de UI y lógica
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var recordarSesion by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Auto-login con la sesión guardada
    LaunchedEffect(Unit) {
        if (SesionLocal.haySesionGuardada(context)) {
            val sesion = SesionLocal.obtenerSesionGuardada(context)
            if (sesion != null && sesion.refreshToken.isNotBlank()) {
                cargando = true
                val resultado = SupabaseApi.refrescarSesion(sesion.refreshToken)
                resultado.onSuccess { nuevaSesion ->
                    SesionLocal.actualizarTokens(
                        context = context,
                        accessToken = nuevaSesion.accessToken,
                        refreshToken = nuevaSesion.refreshToken
                    )
                    cargando = false
                    onLoginExitoso()
                }
                resultado.onFailure {
                    SesionLocal.cerrarSesion(context)
                    cargando = false
                }
            }
        }
    }

    fun iniciarSesion() {
        if (cargando) return
        when {
            correo.isBlank() || password.isBlank() -> {
                error = "Ingresa tu correo y contraseña"
            }
            else -> {
                error = ""
                cargando = true
                scope.launch {
                    val resultado = SupabaseApi.iniciarSesion(
                        correo = correo.trim(),
                        password = password
                    )
                    resultado.onSuccess { sesion ->
                        SesionLocal.guardarSesion(
                            context = context,
                            sesion = sesion,
                            recordar = recordarSesion
                        )
                        cargando = false
                        onLoginExitoso()
                    }
                    resultado.onFailure { exception ->
                        cargando = false
                        error = exception.message ?: "Correo o contraseña incorrectos"
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(FondoPrincipal)) {

        // 1. Fondo con el logo y la ciudad (ya trae el título y el eslogan)
        Image(
            painter = painterResource(id = R.drawable.background_login),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Degradado suave para que el formulario siempre se lea bien
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.45f to Color.Transparent,
                        0.75f to FondoPrincipal.copy(alpha = .85f),
                        1f to FondoPrincipal
                    )
                )
        )

        // 3. Formulario: con scroll y a prueba de teclado
        // Pegado abajo: si no cabe (teclado abierto o pantalla chica) se desplaza.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = CardOscura.copy(alpha = .94f),
                border = BorderStroke(1.dp, BordeSuave)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Bienvenido de nuevo", color = TextoPrincipal, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                    CampoIconoCW(
                        value = correo,
                        onChange = { correo = it },
                        placeholder = "Correo electrónico",
                        icono = Icons.Outlined.Email,
                        keyboardType = KeyboardType.Email,
                        habilitado = !cargando,
                        contenedor = FondoSecundario
                    )

                    CampoIconoCW(
                        value = password,
                        onChange = { password = it },
                        placeholder = "Contraseña",
                        icono = Icons.Outlined.Lock,
                        esPassword = true,
                        imeAction = ImeAction.Done,
                        habilitado = !cargando,
                        contenedor = FondoSecundario,
                        onHecho = { iniciarSesion() }
                    )

                    // Recordar sesión + ¿Olvidaste tu contraseña?
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = recordarSesion,
                            onCheckedChange = { recordarSesion = it },
                            enabled = !cargando,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MoradoPrincipal,
                                uncheckedColor = TextoSuave,
                                checkmarkColor = Color.White
                            )
                        )
                        Text(
                            text = "Recordarme",
                            color = TextoSecundario,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !cargando) { recordarSesion = !recordarSesion }
                        )
                        TextButton(onClick = { if (!cargando) onOlvidoPassword() }, enabled = !cargando) {
                            Text("¿Olvidaste tu contraseña?", color = MoradoClaro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (error.isNotEmpty()) AvisoCW(error, TipoAviso.ERROR)

                    BotonCW(
                        texto = "Iniciar sesión",
                        icono = Icons.Default.ArrowForward,
                        cargando = cargando,
                        onClick = { iniciarSesion() }
                    )

                    // Separador
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BordeSuave)
                        Text(
                            " o continúa con ",
                            color = TextoSuave,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = BordeSuave)
                    }

                    // Google (con la lógica de Intent de siempre)
                    OutlinedButton(
                        onClick = {
                            if (!cargando) {
                                val url = SupabaseApi.obtenerUrlGoogleOAuth()
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = FondoSecundario,
                            contentColor = TextoPrincipal
                        ),
                        border = BorderStroke(1.dp, BordeSuave),
                        enabled = !cargando
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google_logo),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Continuar con Google", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Crear cuenta
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿No tienes cuenta? ", color = TextoSecundario, fontSize = 14.sp)
                Text(
                    text = "Crear cuenta",
                    color = MoradoClaro,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !cargando) { onCrearCuenta() }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
