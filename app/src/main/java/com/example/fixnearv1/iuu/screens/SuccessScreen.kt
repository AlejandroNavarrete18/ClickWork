package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.R
import com.example.fixnearv1.modelo.ui.theme.*

@Composable
fun SuccessScreen(
    emailUsuario: String,
    onNavigateToLogin: () -> Unit,
    onBackClick: () -> Unit
) {
    val backgroundColor = FondoPrincipal
    val purpleAccent = MoradoPrincipal
    val cardBackground = Color.White
    val textColor = TextoPrincipal
    val textSecondary = TextoSecundario

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp), // Margen general equilibrado
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Flecha de retroceso
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = purpleAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Espacio equilibrado

        // Imagen del buzón (Reducida un poco para que no se coma toda la pantalla)
        Image(
            painter = painterResource(id = R.drawable.email_success),
            contentDescription = "Correo enviado con éxito",
            modifier = Modifier.size(160.dp) // <-- Ajuste clave aquí
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "¡Listo!",
            color = textColor,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Revisa tu correo electrónico",
            color = purpleAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hemos enviado las instrucciones\npara restablecer tu contraseña a:",
            color = textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = emailUsuario,
            color = purpleAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        // 👇 LA SOLUCIÓN AL AMONTONAMIENTO 👇
        // Primero forzamos un mínimo de 32.dp de separación
        Spacer(modifier = Modifier.height(32.dp))
        // Y luego dejamos que el weight empuje el resto hacia abajo si sobra espacio
        Spacer(modifier = Modifier.weight(1f))

        // Tarjeta de Spam
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Revisar Spam",
                    tint = purpleAccent,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "¿No ves el correo?",
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Revisa tu bandeja de spam o\ncorreo no deseado.",
                        color = textSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Botón "Volver al inicio"
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = purpleAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Volver al inicio",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Inicio",
                tint = Color.White
            )
        }
    }
}