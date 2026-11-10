package com.example.fixnearv1.modelo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClickWorkLight = lightColorScheme(
    primary = MoradoPrincipal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE8F8),
    onPrimaryContainer = Color(0xFF163B68),
    secondary = AzulAcento,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F2ED),
    onSecondaryContainer = Color(0xFF075B50),
    tertiary = Ambar,
    onTertiary = Color.White,
    background = FondoPrincipal,
    onBackground = TextoPrincipal,
    surface = CardOscura,
    onSurface = TextoPrincipal,
    surfaceVariant = CardOscura2,
    onSurfaceVariant = TextoSecundario,
    outline = BordeSuave,
    outlineVariant = BordeSuave,
    error = Peligro,
    onError = Color.White
)

@Composable
fun FixNearV1Theme(darkTheme: Boolean = false, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClickWorkLight, typography = Typography, content = content)
}
