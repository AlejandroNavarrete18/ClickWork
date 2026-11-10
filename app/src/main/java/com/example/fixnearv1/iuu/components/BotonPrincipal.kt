package com.example.fixnearv1.iuu.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fixnearv1.modelo.ui.theme.ClickWorkPurpleMain

@Composable
fun BotonPrincipal(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconLeft: Int? = null,
    iconRight: Int? = null
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = ClickWorkPurpleMain // Usa el morado que creamos
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp), // Altura estándar y elegante para botones
        shape = MaterialTheme.shapes.large // Bordes redondeados
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Icono izquierdo (si se le manda uno)
            if (iconLeft != null) {
                Icon(
                    painter = painterResource(id = iconLeft),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterStart),
                    tint = Color.White
                )
            }

            // Texto centrado
            Text(
                text = text,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )

            // Icono derecho (si se le manda uno)
            if (iconRight != null) {
                Icon(
                    painter = painterResource(id = iconRight),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    tint = Color.White
                )
            }
        }
    }
}