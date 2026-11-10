package com.example.fixnearv1.iuu.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fixnearv1.modelo.ui.theme.*

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label, color = TextoSuave) },
        placeholder = { Text(text = placeholder, color = TextoSuave) },
        leadingIcon = leadingIcon,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ClickWorkPurpleMain,
            unfocusedBorderColor = BordeSuave,
            focusedTextColor = TextoPrincipal,
            unfocusedTextColor = TextoPrincipal,
            focusedLabelColor = ClickWorkPurpleMain,
            unfocusedLabelColor = TextoSuave,
            focusedContainerColor = FondoSecundario,
            unfocusedContainerColor = FondoSecundario,
            cursorColor = ClickWorkPurpleMain
        )
    )
}