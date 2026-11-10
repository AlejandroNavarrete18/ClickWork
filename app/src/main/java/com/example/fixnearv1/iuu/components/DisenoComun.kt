package com.example.fixnearv1.iuu.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.fixnearv1.modelo.ui.theme.*

/** Cabecera común de pantalla: botón atrás redondo + título + subtítulo. */
@Composable
fun CabeceraPantalla(
    titulo: String,
    subtitulo: String? = null,
    onRegresar: (() -> Unit)? = null,
    acciones: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onRegresar != null) {
            Surface(shape = CircleShape, color = CardOscura, border = BorderStroke(1.dp, BordeSuave)) {
                IconButton(onClick = onRegresar, modifier = Modifier.size(42.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = TextoPrincipal, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = TextoPrincipal, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) Text(subtitulo, color = TextoSuave, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        acciones()
    }
}

/** Cuadro de icono con fondo tintado. */
@Composable
fun IconoCaja(icon: ImageVector, tint: Color, tam: Dp = 42.dp) {
    Box(
        Modifier.size(tam).background(tint.copy(alpha = .16f), RoundedCornerShape(tam * 0.31f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(tam * 0.52f)) }
}

/** Tarjeta con título e icono para agrupar contenido. */
@Composable
fun TarjetaSeccion(
    titulo: String,
    icono: ImageVector,
    tint: Color = MoradoClaro,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardOscura,
        border = BorderStroke(1.dp, BordeSuave)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconoCaja(icono, tint, 34.dp)
                Spacer(Modifier.width(10.dp))
                Text(titulo, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            content()
        }
    }
}

/** Etiqueta pequeña tipo "píldora". */
@Composable
fun Pastilla(texto: String, color: Color, icono: ImageVector? = null) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = .15f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) { Icon(icono, null, tint = color, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)) }
            Text(texto, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun ChipCW(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (seleccionado) MoradoPrincipal else CardOscura2,
        border = BorderStroke(1.dp, if (seleccionado) MoradoPrincipal else BordeSuave)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (seleccionado) { Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)) }
            Text(texto, color = if (seleccionado) Color.White else TextoSecundario, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Elegir UNA opción. Tocar la seleccionada la deja vacía si permiteVacio. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsUnicos(opciones: List<String>, seleccionado: String, permiteVacio: Boolean = true, onSeleccion: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { op ->
            ChipCW(op, op == seleccionado) { onSeleccion(if (op == seleccionado && permiteVacio) "" else op) }
        }
    }
}

/** Elegir VARIAS opciones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipsMultiples(opciones: List<String>, seleccion: Set<String>, onToggle: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opciones.forEach { op -> ChipCW(op, op in seleccion) { onToggle(op) } }
    }
}

/** Campo de texto con el estilo de ClickWork. */
@Composable
fun CampoCW(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    maxChars: Int = 0,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(if (maxChars > 0) it.take(maxChars) else it) },
        label = { Text(label) },
        placeholder = if (placeholder.isNotBlank()) ({ Text(placeholder, color = TextoSuave) }) else null,
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(14.dp),
        supportingText = if (maxChars > 0 && minLines > 1) ({ Text("${value.length}/$maxChars", color = TextoSuave, fontSize = 11.sp) }) else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = FondoSecundario, unfocusedContainerColor = FondoSecundario,
            focusedTextColor = TextoPrincipal, unfocusedTextColor = TextoPrincipal,
            focusedBorderColor = MoradoPrincipal, unfocusedBorderColor = BordeSuave,
            focusedLabelColor = MoradoClaro, unfocusedLabelColor = TextoSuave,
            cursorColor = MoradoClaro
        )
    )
}

/** Círculo con la inicial del nombre. */
@Composable
fun AvatarInicial(nombre: String, tam: Dp = 48.dp) {
    val inicial = nombre.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        Modifier.size(tam).background(Brush.linearGradient(listOf(MoradoSecundario, AzulAcento)), CircleShape),
        contentAlignment = Alignment.Center
    ) { Text(inicial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (tam.value * 0.42f).sp) }
}

/** Estado vacío amable. */
@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconoCaja(icono, MoradoClaro, 56.dp)
        Text(titulo, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Text(texto, color = TextoSuave, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

/** Selector − N +. */
@Composable
fun ContadorCW(valor: Int, min: Int = 1, max: Int = 99, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(shape = CircleShape, color = CardOscura2, border = BorderStroke(1.dp, BordeSuave)) {
            IconButton(onClick = { if (valor > min) onChange(valor - 1) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Remove, "Menos", tint = TextoPrincipal)
            }
        }
        Text("$valor", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Surface(shape = CircleShape, color = MoradoPrincipal) {
            IconButton(onClick = { if (valor < max) onChange(valor + 1) }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, "Más", tint = Color.White)
            }
        }
    }
}

/** Fila de interruptor con título y descripción. */
@Composable
fun InterruptorCW(titulo: String, descripcion: String, activo: Boolean, onCambio: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, color = TextoPrincipal, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(descripcion, color = TextoSuave, fontSize = 12.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = activo, onCheckedChange = onCambio,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = MoradoPrincipal,
                uncheckedThumbColor = TextoSuave, uncheckedTrackColor = CardOscura2, uncheckedBorderColor = BordeSuave
            )
        )
    }
}


// ─────────────────────────────────────────────────────────────
//  Componentes añadidos en la entrega "Pendientes V3"
//  (Perfil, Mis solicitudes, Mapa, Modo trabajador, Login y Registro)
// ─────────────────────────────────────────────────────────────

/** Tipo de aviso en banda. */
enum class TipoAviso { ERROR, EXITO, INFO }

/** Aviso en banda con icono: errores de formulario, confirmaciones o pistas. */
@Composable
fun AvisoCW(texto: String, tipo: TipoAviso = TipoAviso.ERROR, modifier: Modifier = Modifier) {
    val color = when (tipo) { TipoAviso.ERROR -> Peligro; TipoAviso.EXITO -> Exito; TipoAviso.INFO -> AzulAcento }
    val icono = when (tipo) { TipoAviso.ERROR -> Icons.Default.Error; TipoAviso.EXITO -> Icons.Default.CheckCircle; TipoAviso.INFO -> Icons.Default.Info }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = .12f),
        border = BorderStroke(1.dp, color.copy(alpha = .4f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(texto, color = color, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
    }
}

/** Botón principal de ClickWork (violeta). Muestra una ruedita mientras carga. */
@Composable
fun BotonCW(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    cargando: Boolean = false,
    habilitado: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !cargando,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MoradoPrincipal,
            disabledContainerColor = MoradoPrincipal.copy(alpha = .45f),
            disabledContentColor = Color.White.copy(alpha = .8f)
        )
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            if (icono != null) { Icon(icono, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
            Text(texto, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/** Botón con contorno (acción secundaria). */
@Composable
fun BotonSecundarioCW(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    habilitado: Boolean = true,
    color: Color = MoradoClaro
) {
    OutlinedButton(
        onClick = onClick,
        enabled = habilitado,
        modifier = modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (habilitado) BordeSuave else BordeSuave.copy(alpha = .5f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = CardOscura, contentColor = color)
    ) {
        if (icono != null) { Icon(icono, null, tint = color, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(texto, color = color, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1)
    }
}

/**
 * Campo de una sola línea con icono a la izquierda (Login y Registro).
 * Si esPassword es true, agrega el ojo para mostrar/ocultar la contraseña.
 */
@Composable
fun CampoIconoCW(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    esPassword: Boolean = false,
    habilitado: Boolean = true,
    esError: Boolean = false,
    contenedor: Color = FondoSecundario,
    onHecho: (() -> Unit)? = null
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = TextoSuave) },
        leadingIcon = { Icon(icono, null, tint = MoradoClaro) },
        trailingIcon = if (esPassword) ({
            IconButton(onClick = { visible = !visible }, enabled = habilitado) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = TextoSuave
                )
            }
        }) else null,
        visualTransformation = if (esPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (esPassword) KeyboardType.Password else keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onHecho?.invoke() }),
        singleLine = true,
        enabled = habilitado,
        isError = esError,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = contenedor, unfocusedContainerColor = contenedor, disabledContainerColor = contenedor,
            focusedTextColor = TextoPrincipal, unfocusedTextColor = TextoPrincipal, disabledTextColor = TextoSuave,
            focusedBorderColor = MoradoPrincipal, unfocusedBorderColor = BordeSuave,
            errorBorderColor = Peligro, errorContainerColor = contenedor,
            cursorColor = MoradoClaro
        )
    )
}

/** Fila de ajustes: icono, título, valor opcional y flecha. Úsala dentro de una tarjeta. */
@Composable
fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    modifier: Modifier = Modifier,
    valor: String = "",
    tint: Color = MoradoClaro,
    pronto: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clic = if (onClick != null && !pronto) Modifier.clickable { onClick() } else Modifier
    Row(
        modifier.fillMaxWidth().then(clic).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconoCaja(icono, if (pronto) TextoSuave else tint, 36.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            titulo,
            color = if (pronto) TextoSuave else TextoPrincipal,
            fontSize = 15.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (pronto) {
            Pastilla("Pronto", TextoSuave)
        } else {
            if (valor.isNotBlank()) {
                Text(valor, color = TextoSuave, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
                Spacer(Modifier.width(4.dp))
            }
            if (onClick != null) Icon(Icons.Default.ChevronRight, null, tint = TextoSuave)
        }
    }
}

/** Tarjeta contenedora de varias FilaAjuste, con divisores entre filas. */
@Composable
fun TarjetaAjustes(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = CardOscura,
        border = BorderStroke(1.dp, BordeSuave)
    ) { Column(content = content) }
}

/** Divisor fino para separar filas dentro de TarjetaAjustes. */
@Composable
fun DivisorCW() {
    HorizontalDivider(Modifier.padding(start = 64.dp), color = BordeSuave.copy(alpha = .6f))
}

/** Diálogo con el estilo de ClickWork. Si textoCancelar es null no muestra el botón de cancelar. */
@Composable
fun DialogoCW(
    titulo: String,
    onCerrar: () -> Unit,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    modifier: Modifier = Modifier,
    textoCancelar: String? = "Cancelar",
    confirmarHabilitado: Boolean = true,
    cargando: Boolean = false,
    colorConfirmar: Color = MoradoClaro,
    contenido: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!cargando) onCerrar() },
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        containerColor = CardOscura,
        title = { Text(titulo, color = TextoPrincipal, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = contenido) },
        confirmButton = {
            TextButton(onClick = onConfirmar, enabled = confirmarHabilitado && !cargando) {
                if (cargando) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MoradoClaro, strokeWidth = 2.dp)
                else Text(textoConfirmar, color = colorConfirmar, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = if (textoCancelar != null) ({
            TextButton(onClick = onCerrar, enabled = !cargando) { Text(textoCancelar, color = TextoSecundario) }
        }) else null
    )
}
