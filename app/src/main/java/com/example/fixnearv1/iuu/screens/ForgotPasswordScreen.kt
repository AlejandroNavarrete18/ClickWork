package com.example.fixnearv1.iuu.screens

import android.util.Patterns // <-- IMPORTANTE PARA VALIDAR EL CORREO
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.R
import com.example.fixnearv1.iuu.components.BotonPrincipal
import com.example.fixnearv1.iuu.components.CustomTextField
import com.example.fixnearv1.modelo.ui.theme.*

@Composable
fun ForgotPasswordScreen(
    onRegresar: () -> Unit,
    onEnviarInstrucciones: (String) -> Unit
) {
    val backgroundColor = FondoPrincipal
    val textColor = TextoPrincipal
    val subtitleColor = TextoSecundario
    val bannerBackground = Color.White
    val errorColor = Peligro

    var email by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf<String?>(null) } // <-- ESTADO PARA EL ERROR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = { onRegresar() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = ClickWorkPurpleMain
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_clickwork_logo),
            contentDescription = "Logo ClickWork",
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "¿Olvidaste tu contraseña?",
            color = textColor,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No te preocupes, sucede. Ingresa tu correo electrónico y te enviaremos instrucciones para restablecer tu contraseña.",
            color = subtitleColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.forgot_password_illustration),
            contentDescription = "Ilustración Enviar Correo",
            modifier = Modifier
                .height(140.dp)
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Input de Correo
        CustomTextField(
            value = email,
            onValueChange = {
                email = it
                mensajeError = null // <-- Oculta el error en cuanto el usuario empieza a escribir
            },
            label = "Correo electrónico",
            placeholder = "ejemplo@correo.com",
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    tint = ClickWorkPurpleMain
                )
            }
        )

        // TEXTO DE ERROR DINÁMICO
        if (mensajeError != null) {
            Text(
                text = mensajeError!!,
                color = errorColor,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp), // Lo alineamos ligeramente con el TextField
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // BOTÓN CON VALIDACIÓN
        BotonPrincipal(
            text = "Enviar instrucciones",
            onClick = {
                // 1. Verificamos si está vacío
                if (email.trim().isEmpty()) {
                    mensajeError = "Por favor, ingresa tu correo electrónico."
                }
                // 2. Verificamos si tiene formato válido (ej. juan@gmail.com)
                else if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    mensajeError = "Ingresa un formato de correo válido."
                }
                // 3. Si todo está bien, enviamos la instrucción
                else {
                    mensajeError = null
                    onEnviarInstrucciones(email.trim())
                }
            },
            iconRight = R.drawable.ic_arrow_right
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "¿Recordaste tu contraseña? ", color = subtitleColor, fontSize = 14.sp)
            Text(
                text = "Iniciar sesión",
                color = ClickWorkPurpleMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable {
                    onRegresar()
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerBackground, shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Seguridad",
                tint = ClickWorkPurpleMain,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = "Tu información está segura", color = textColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "Protegemos tus datos personales y nunca compartimos tu información.",
                    color = subtitleColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}