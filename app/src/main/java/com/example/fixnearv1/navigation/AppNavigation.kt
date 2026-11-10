package com.example.fixnearv1.navigation

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.fixnearv1.MainActivity
import com.example.fixnearv1.iuu.screens.*
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SesionSupabase
import com.example.fixnearv1.utils.SupabaseApi
import com.example.fixnearv1.utils.Vacante

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun AppNavigation() {

    var pantallaActual by remember {
        mutableStateOf("splash")
    }

    var vacanteSeleccionada by remember {
        mutableStateOf<Vacante?>(null)
    }

    var correoRecuperacion by remember {
        mutableStateOf("")
    }

    var trabajadorSeleccionadoId by remember {
        mutableStateOf("")
    }

    val context = LocalContext.current

    // Procesar deep link de OAuth con Google.
    // Nada de lo que viene en el link es confiable: el servidor valida el token
    // y nos dice quién es el usuario.
    val intentPendiente = MainActivity.pendingIntent

    LaunchedEffect(intentPendiente) {
        val data: Uri = intentPendiente?.data ?: return@LaunchedEffect

        if (data.scheme != "fixnear" || data.host != "auth") return@LaunchedEffect

        val fragment = data.fragment ?: return@LaunchedEffect

        val params = fragment.split("&").mapNotNull {
            val partes = it.split("=", limit = 2)

            if (partes.size == 2) {
                Uri.decode(partes[0]) to Uri.decode(partes[1])
            } else {
                null
            }
        }.toMap()

        val accessToken = params["access_token"].orEmpty()
        val refreshToken = params["refresh_token"].orEmpty()

        try {
            if (accessToken.isBlank()) return@LaunchedEffect

            val usuario = SupabaseApi
                .obtenerUsuarioAutenticado(accessToken)
                .getOrNull()
                ?: return@LaunchedEffect

            SesionLocal.guardarSesion(
                context = context,
                sesion = SesionSupabase(
                    userId = usuario.userId,
                    correo = usuario.correo,
                    accessToken = accessToken,
                    refreshToken = refreshToken
                ),
                recordar = true
            )

            // Los usuarios de Google no pasan por el registro: se crea su perfil aquí
            SupabaseApi.asegurarPerfil(
                accessToken = accessToken,
                userId = usuario.userId,
                correo = usuario.correo
            )

            pantallaActual = "menu"
        } finally {
            // Consumimos el intent al final para no cancelar este mismo efecto
            MainActivity.pendingIntent = null
        }
    }

    when (pantallaActual) {

        "splash" -> SplashScreen(
            onNavigateToLogin = {
                pantallaActual = "login"
            }
        )

        "login" -> LoginScreen(
            onLoginExitoso = {
                pantallaActual = "menu"
            },
            onCrearCuenta = {
                pantallaActual = "registro"
            },
            onOlvidoPassword = {
                pantallaActual = "recuperar_password"
            }
        )

        "recuperar_password" -> ForgotPasswordScreen(
            onRegresar = {
                pantallaActual = "login"
            },
            onEnviarInstrucciones = { correoIngresado ->
                correoRecuperacion = correoIngresado
                pantallaActual = "success"
            }
        )

        "success" -> SuccessScreen(
            emailUsuario = correoRecuperacion,
            onNavigateToLogin = {
                pantallaActual = "login"
                correoRecuperacion = ""
            },
            onBackClick = {
                pantallaActual = "recuperar_password"
                correoRecuperacion = ""
            }
        )

        "registro" -> RegisterScreen(
            onCuentaCreada = {
                pantallaActual = "verificacion"
            },
            onRegresar = {
                pantallaActual = "login"
            }
        )

        "verificacion" -> VerifyEmailScreen(
            onVerificado = {
                pantallaActual = "menu"
            },
            onRegresar = {
                pantallaActual = "registro"
            }
        )

        "menu" -> ClickWorkShell(
            onCerrarSesion = {
                SesionLocal.cerrarSesion(context)
                pantallaActual = "login"
            },
            onQr = { pantallaActual = "qr" },
            onCompany = { pantallaActual = "centroEmpresas" },
            onWorkerProfile = { trabajadorId ->
                trabajadorSeleccionadoId = trabajadorId
                pantallaActual = "perfilTrabajador"
            }
        )

        "servicios" -> ServiciosScreen(
            onRegresar = {
                pantallaActual = "menu"
            },
            onVerPerfilTrabajador = { trabajadorId ->
                trabajadorSeleccionadoId = trabajadorId
                pantallaActual = "perfilTrabajador"
            }
        )

        "perfilTrabajador" -> {
            if (trabajadorSeleccionadoId.isNotBlank()) {
                PerfilTrabajadorScreen(
                    trabajadorId = trabajadorSeleccionadoId,
                    onRegresar = {
                        pantallaActual = "servicios"
                    }
                )
            } else {
                pantallaActual = "servicios"
            }
        }

        "empleos" -> EmpleosScreen(
            onRegresar = {
                pantallaActual = "menu"
            },
            onVerDetalle = { vacante ->
                vacanteSeleccionada = vacante
                pantallaActual = "detalleEmpleo"
            }
        )

        "detalleEmpleo" -> {
            if (vacanteSeleccionada != null) {
                DetalleVacanteScreen(
                    vacante = vacanteSeleccionada!!,
                    onRegresar = {
                        pantallaActual = "empleos"
                        vacanteSeleccionada = null
                    }
                )
            } else {
                pantallaActual = "empleos"
            }
        }

        "centroEmpresas" -> CentroEmpresasScreen(
            onRegresar = { pantallaActual = "menu" }
        )

        "qr" -> QrScannerScreen(
            onRegresar = {
                pantallaActual = "menu"
            }
        )

        "perfil" -> PerfilScreen(
            onRegresar = {
                pantallaActual = "menu"
            },
            onCerrarSesion = {
                pantallaActual = "login"
            }
        )
    }
}