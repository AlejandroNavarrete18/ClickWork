package com.example.fixnearv1.iuu.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.fixnearv1.modelo.ui.theme.ClickWorkTextSecondary

@Composable
fun BotonSecundario(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconLeft: Int? = null,
    iconRight: Int? = null
) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, ClickWorkTextSecondary), // Contorno gris
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (iconLeft != null) {
                Icon(
                    painter = painterResource(id = iconLeft),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterStart),
                    tint = ClickWorkTextSecondary
                )
            }

            Text(
                text = text,
                color = ClickWorkTextSecondary,
                style = MaterialTheme.typography.titleMedium
            )

            if (iconRight != null) {
                Icon(
                    painter = painterResource(id = iconRight),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    tint = ClickWorkTextSecondary
                )
            }
        }
    }
}