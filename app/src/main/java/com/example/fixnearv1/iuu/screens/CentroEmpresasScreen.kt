package com.example.fixnearv1.iuu.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fixnearv1.iuu.components.*
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.Postulante
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import com.example.fixnearv1.utils.Vacante
import kotlinx.coroutines.launch

private val CATEGORIAS = listOf("Comercio", "Restaurantes", "Construcción", "Hogar", "Transporte", "Oficina", "Salud", "Educación", "Tecnología", "Otros")
private val PERIODOS = listOf("Por hora", "Diario", "Semanal", "Quincenal", "Mensual", "Por proyecto", "Comisión")
private val METODOS = listOf("Efectivo", "Transferencia", "Depósito", "Billetera digital", "Cheque")
private val JORNADAS = listOf("Tiempo completo", "Medio tiempo", "Por turnos", "Fines de semana", "Por proyecto")
private val MODALIDADES = listOf("Presencial", "Híbrido", "Remoto")
private val DOCUMENTOS = listOf("CV", "Carta de recomendación", "Identificación oficial", "Comprobante de domicilio", "Portafolio")
private val PASOS = listOf("Puesto", "Pago", "Detalles", "Postulación")

@Composable
fun CentroEmpresasScreen(onRegresar: () -> Unit) {
    var pestana by remember { mutableStateOf(0) }
    var verPostulantesDe by remember { mutableStateOf<Vacante?>(null) }

    Column(Modifier.fillMaxSize().background(FondoPrincipal)) {
        val vac = verPostulantesDe
        if (vac != null) {
            PostulantesPanel(vac, onRegresar = { verPostulantesDe = null })
            return@Column
        }

        CabeceraPantalla("Publicar vacante", "Encuentra talento como particular o empresa.", onRegresar)

        // Selector de pestañas
        Surface(
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            shape = RoundedCornerShape(50), color = CardOscura, border = BorderStroke(1.dp, BordeSuave)
        ) {
            Row(Modifier.padding(4.dp)) {
                listOf("Nueva vacante", "Mis vacantes").forEachIndexed { i, t ->
                    Surface(
                        onClick = { pestana = i },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        color = if (pestana == i) MoradoPrincipal else Color.Transparent
                    ) {
                        Text(
                            t, Modifier.padding(vertical = 10.dp), color = if (pestana == i) Color.White else TextoSecundario,
                            fontWeight = FontWeight.SemiBold, fontSize = 14.sp, textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (pestana == 0) AsistentePublicar(onPublicada = { pestana = 1 })
        else MisVacantesPanel(onVerPostulantes = { verPostulantesDe = it })
    }
}

// ───────────────────────── Asistente de 4 pasos ─────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AsistentePublicar(onPublicada: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var paso by remember { mutableStateOf(0) }
    // Paso 1
    var puesto by remember { mutableStateOf("") }
    var empresa by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("") }
    var plazas by remember { mutableStateOf(1) }
    var urgente by remember { mutableStateOf(false) }
    // Paso 2
    var salario by remember { mutableStateOf("") }
    var periodo by remember { mutableStateOf("") }
    var metodos by remember { mutableStateOf(setOf<String>()) }
    var beneficios by remember { mutableStateOf("") }
    // Paso 3
    var jornada by remember { mutableStateOf("") }
    var modalidad by remember { mutableStateOf("") }
    var horario by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var requisitos by remember { mutableStateOf("") }
    // Paso 4
    var documentos by remember { mutableStateOf(setOf<String>("CV")) }
    var permiteCv by remember { mutableStateOf(true) }
    var telefono by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }

    var enviando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var exito by remember { mutableStateOf(false) }

    fun validar(p: Int): String = when (p) {
        0 -> if (puesto.isBlank() || empresa.isBlank()) "Escribe el puesto y el nombre del negocio o empleador." else ""
        1 -> if (salario.isBlank()) "Indica el sueldo (puede ser un rango o \"a convenir\")." else ""
        2 -> if (horario.isBlank() || descripcion.isBlank()) "Completa horario y descripción del trabajo." else ""
        else -> ""
    }

    fun publicar() {
        val token = SesionLocal.obtenerAccessToken(context)
        if (token.isBlank()) { error = "Tu sesión no está activa. Inicia sesión nuevamente."; return }
        for (p in 0..2) { val e = validar(p); if (e.isNotBlank()) { error = e; paso = p; return } }
        enviando = true; error = ""
        scope.launch {
            val r = SupabaseApi.crearVacante(
                accessToken = token,
                userId = SesionLocal.obtenerUserId(context),
                empresa = empresa.trim(), puesto = puesto.trim(), salario = salario.trim(),
                horario = horario.trim(), descripcion = descripcion.trim(),
                requisitos = requisitos.trim(), beneficios = beneficios.trim(),
                telefono = telefono.trim(), direccion = direccion.trim(),
                categoria = categoria, plazas = plazas, periodoPago = periodo,
                metodosPago = METODOS.filter { it in metodos },
                documentosRequeridos = DOCUMENTOS.filter { it in documentos },
                permiteCv = permiteCv, urgente = urgente,
                tipoJornada = jornada, modalidad = modalidad
            )
            enviando = false
            r.onSuccess { exito = true }.onFailure { error = it.message ?: "No se pudo publicar. Revisa Supabase y las políticas RLS." }
        }
    }

    if (exito) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            IconoCaja(Icons.Default.CheckCircle, Exito, 80.dp)
            Spacer(Modifier.height(16.dp))
            Text("¡Vacante publicada!", color = TextoPrincipal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Ya aparece en Empleos. Te avisaremos aquí cuando lleguen postulantes.", color = TextoSuave, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(20.dp))
            Button(onClick = onPublicada, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal)) {
                Text("Ver mis vacantes", fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        // Indicador de pasos
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PASOS.forEachIndexed { i, t ->
                    val activo = i <= paso
                    Text(t, color = if (activo) MoradoClaro else TextoSuave, fontSize = 12.sp, fontWeight = if (i == paso) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { (paso + 1) / PASOS.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MoradoPrincipal, trackColor = CardOscura2, strokeCap = StrokeCap.Round
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (paso) {
                0 -> {
                    TarjetaSeccion("¿Qué puesto buscas cubrir?", Icons.Default.Work) {
                        CampoCW("Puesto", puesto, placeholder = "Ej. Cajero, Ayudante de cocina…", maxChars = 80) { puesto = it }
                        CampoCW("Negocio o empleador", empresa, placeholder = "Ej. Tacos El Güero / Tu nombre", maxChars = 80) { empresa = it }
                    }
                    TarjetaSeccion("Categoría", Icons.Default.Category, Menta) {
                        ChipsUnicos(CATEGORIAS, categoria) { categoria = it }
                    }
                    TarjetaSeccion("Plazas y urgencia", Icons.Default.Group, Ambar) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("Personas que necesitas", color = TextoSecundario, modifier = Modifier.weight(1f))
                            ContadorCW(plazas, 1, 99) { plazas = it }
                        }
                        InterruptorCW("Contratación urgente", "Se muestra con una etiqueta destacada.", urgente) { urgente = it }
                    }
                }
                1 -> {
                    TarjetaSeccion("Sueldo", Icons.Default.Payments, Exito) {
                        CampoCW("Sueldo", salario, placeholder = "Ej. $9,000 o \$8,000–\$10,000", maxChars = 40) { salario = it }
                        Text("¿Cada cuándo se paga?", color = TextoSuave, fontSize = 13.sp)
                        ChipsUnicos(PERIODOS, periodo) { periodo = it }
                    }
                    TarjetaSeccion("Formas de pago", Icons.Default.AccountBalanceWallet, AzulAcento) {
                        Text("Elige todas las que aceptas. Los candidatos las verán antes de postularse.", color = TextoSuave, fontSize = 13.sp)
                        ChipsMultiples(METODOS, metodos) { m -> metodos = if (m in metodos) metodos - m else metodos + m }
                    }
                    TarjetaSeccion("Prestaciones y bonos", Icons.Default.CardGiftcard, Ambar) {
                        CampoCW("Prestaciones (opcional)", beneficios, minLines = 2, maxChars = 400, placeholder = "Propinas, comidas, bonos, transporte…") { beneficios = it }
                    }
                }
                2 -> {
                    TarjetaSeccion("Jornada y modalidad", Icons.Default.Schedule, MoradoClaro) {
                        Text("Tipo de jornada", color = TextoSuave, fontSize = 13.sp)
                        ChipsUnicos(JORNADAS, jornada) { jornada = it }
                        Text("Modalidad", color = TextoSuave, fontSize = 13.sp)
                        ChipsUnicos(MODALIDADES, modalidad) { modalidad = it }
                        CampoCW("Horario", horario, placeholder = "Ej. Lun–Sáb 9:00 a 17:00", maxChars = 80) { horario = it }
                    }
                    TarjetaSeccion("Sobre el trabajo", Icons.Default.Description, Menta) {
                        CampoCW("Descripción", descripcion, minLines = 4, maxChars = 800, placeholder = "¿Qué hará la persona? ¿Cómo es el equipo?") { descripcion = it }
                        CampoCW("Requisitos (opcional)", requisitos, minLines = 2, maxChars = 400, placeholder = "Experiencia, edad, herramientas…") { requisitos = it }
                    }
                }
                else -> {
                    TarjetaSeccion("Documentos que pides", Icons.Default.FolderOpen, AzulAcento) {
                        Text("Los candidatos verán esta lista antes de postularse.", color = TextoSuave, fontSize = 13.sp)
                        ChipsMultiples(DOCUMENTOS, documentos) { d -> documentos = if (d in documentos) documentos - d else documentos + d }
                        InterruptorCW("Permitir adjuntar CV", "PDF o Word, hasta 5 MB. Solo tú lo ves.", permiteCv) { permiteCv = it }
                    }
                    TarjetaSeccion("Contacto y lugar", Icons.Default.LocationOn, Coral) {
                        CampoCW("Teléfono de contacto (opcional)", telefono, keyboardType = KeyboardType.Phone, maxChars = 20) { telefono = it }
                        CampoCW("Dirección o zona de trabajo", direccion, placeholder = "Colonia, calle o zona", maxChars = 120) { direccion = it }
                    }
                    Text("Así la verán los candidatos", color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    VacanteTarjeta(
                        vacante = Vacante(
                            id = "vista-previa", empresa = empresa.ifBlank { "Tu negocio" }, puesto = puesto.ifBlank { "Tu puesto" },
                            salario = salario.ifBlank { "A convenir" }, horario = horario, descripcion = descripcion,
                            requisitos = requisitos, beneficios = beneficios, telefono = telefono, direccion = direccion,
                            latitud = null, longitud = null, disponible = true,
                            categoria = categoria, plazas = plazas, periodoPago = periodo,
                            metodosPago = METODOS.filter { it in metodos }, documentosRequeridos = DOCUMENTOS.filter { it in documentos },
                            permiteCv = permiteCv, urgente = urgente, tipoJornada = jornada, modalidad = modalidad
                        ),
                        onClick = {}
                    )
                }
            }
            if (error.isNotBlank()) {
                Surface(shape = RoundedCornerShape(14.dp), color = Peligro.copy(alpha = .12f), border = BorderStroke(1.dp, Peligro.copy(alpha = .4f))) {
                    Text(error, Modifier.padding(12.dp), color = Peligro, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        // Barra de navegación del asistente
        Surface(color = FondoSecundario, border = BorderStroke(1.dp, BordeSuave)) {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (paso > 0) OutlinedButton(
                    onClick = { error = ""; paso -= 1 },
                    modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, BordeSuave), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal)
                ) { Text("Atrás", fontWeight = FontWeight.SemiBold) }

                Button(
                    onClick = {
                        if (paso < PASOS.lastIndex) {
                            val e = validar(paso)
                            if (e.isBlank()) { error = ""; paso += 1 } else error = e
                        } else publicar()
                    },
                    enabled = !enviando,
                    modifier = Modifier.weight(if (paso > 0) 1.6f else 1f).height(52.dp), shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal, disabledContainerColor = MoradoPrincipal.copy(alpha = .5f))
                ) {
                    if (enviando) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else if (paso < PASOS.lastIndex) { Text("Siguiente", color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); Icon(Icons.Default.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                    else { Icon(Icons.Default.Publish, null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Publicar vacante", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ───────────────────────── Mis vacantes ─────────────────────────

@Composable
private fun MisVacantesPanel(onVerPostulantes: (Vacante) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<Vacante>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    suspend fun cargar() {
        val token = SesionLocal.obtenerAccessToken(context)
        val uid = SesionLocal.obtenerUserId(context)
        SupabaseApi.obtenerMisVacantes(token, uid).onSuccess { lista = it; error = "" }.onFailure { error = it.message ?: "No se pudieron cargar tus vacantes." }
        cargando = false
    }
    LaunchedEffect(Unit) { cargar() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (cargando) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MoradoPrincipal) }
        if (error.isNotBlank()) Text(error, color = Peligro, fontSize = 13.sp)
        if (!cargando && error.isBlank() && lista.isEmpty()) EstadoVacio(Icons.Default.Work, "Aún no publicas vacantes", "Crea la primera en la pestaña “Nueva vacante”.")

        lista.forEach { v ->
            Surface(shape = RoundedCornerShape(22.dp), color = CardOscura, border = BorderStroke(1.dp, BordeSuave)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarInicial(v.empresa, 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(v.puesto, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${v.plazas} plaza(s) · ${v.salario.ifBlank { "A convenir" }}", color = TextoSuave, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        if (v.disponible) Pastilla("Activa", Exito) else Pastilla("Cerrada", TextoSuave)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onVerPostulantes(v) },
                            modifier = Modifier.weight(1.3f), shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MoradoPrincipal)
                        ) { Icon(Icons.Default.Group, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Postulantes", fontWeight = FontWeight.Bold) }
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val token = SesionLocal.obtenerAccessToken(context)
                                    SupabaseApi.cambiarDisponibilidadVacante(token, v.id, !v.disponible)
                                        .onFailure { error = it.message ?: "No se pudo actualizar." }
                                    cargar()
                                }
                            },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BordeSuave),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = if (v.disponible) Ambar else Exito)
                        ) { Text(if (v.disponible) "Cerrar" else "Reabrir", fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
        Spacer(Modifier.height(110.dp))
    }
}

// ───────────────────────── Postulantes ─────────────────────────

@Composable
private fun PostulantesPanel(vacante: Vacante, onRegresar: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<Postulante>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var mensaje by remember { mutableStateOf("") }

    suspend fun cargar() {
        val token = SesionLocal.obtenerAccessToken(context)
        SupabaseApi.obtenerPostulantes(token, vacante.id).onSuccess { lista = it }.onFailure { mensaje = it.message ?: "No se pudieron cargar los postulantes." }
        cargando = false
    }
    LaunchedEffect(vacante.id) { cargar() }

    Column(Modifier.fillMaxSize()) {
        CabeceraPantalla("Postulantes", "${vacante.puesto} · ${lista.size} persona(s)", onRegresar)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (cargando) Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MoradoPrincipal) }
            if (mensaje.isNotBlank()) Text(mensaje, color = Peligro, fontSize = 13.sp)
            if (!cargando && lista.isEmpty() && mensaje.isBlank()) EstadoVacio(Icons.Default.PersonSearch, "Todavía no hay postulantes", "Cuando alguien aplique, lo verás aquí con su CV.")

            lista.forEach { p ->
                val colorEstado = when (p.estado) {
                    "Contratada" -> Exito; "Preseleccionada" -> AzulAcento; "Rechazada" -> Peligro; "Vista" -> MoradoClaro; else -> Ambar
                }
                Surface(shape = RoundedCornerShape(22.dp), color = CardOscura, border = BorderStroke(1.dp, BordeSuave)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AvatarInicial(p.nombre, 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.nombre.ifBlank { "Candidato" }, color = TextoPrincipal, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(p.correo, color = TextoSuave, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Pastilla(p.estado, colorEstado)
                        }
                        if (p.expectativaSalarial.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, null, tint = Exito, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
                            Text("Espera: ${p.expectativaSalarial}", color = Exito, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (p.mensaje.isNotBlank()) Surface(shape = RoundedCornerShape(14.dp), color = CardOscura2) {
                            Text(p.mensaje, Modifier.padding(12.dp), color = TextoSecundario, fontSize = 13.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (p.cvPath.isNotBlank()) OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val token = SesionLocal.obtenerAccessToken(context)
                                        SupabaseApi.urlFirmadaCv(token, p.cvPath)
                                            .onSuccess { url -> runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }
                                            .onFailure { mensaje = it.message ?: "No se pudo abrir el CV." }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, AzulAcento.copy(alpha = .6f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AzulAcento),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) { Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Ver CV", fontSize = 13.sp) }
                            if (p.telefono.isNotBlank()) OutlinedButton(
                                onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${p.telefono}"))) } },
                                shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, BordeSuave),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextoPrincipal),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) { Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Llamar", fontSize = 13.sp) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Preseleccionada" to AzulAcento, "Contratada" to Exito, "Rechazada" to Peligro).forEach { (est, col) ->
                                if (p.estado != est) TextButton(
                                    onClick = {
                                        scope.launch {
                                            val token = SesionLocal.obtenerAccessToken(context)
                                            SupabaseApi.cambiarEstadoPostulacion(token, p.id, est).onFailure { mensaje = it.message ?: "No se pudo cambiar el estado." }
                                            cargar()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text(when (est) { "Preseleccionada" -> "Preseleccionar"; "Contratada" -> "Contratar"; else -> "Rechazar" }, color = col, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(110.dp))
        }
    }
}
