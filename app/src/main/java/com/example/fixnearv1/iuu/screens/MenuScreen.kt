package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Business
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.fixnearv1.utils.PerfilCargador
import com.example.fixnearv1.utils.PerfilUsuario
import com.example.fixnearv1.utils.SesionLocal
import com.example.fixnearv1.utils.SupabaseApi
import com.example.fixnearv1.modelo.ui.theme.*

@Composable
fun MenuScreen(
    onServiciosClick: () -> Unit,
    onPerfilClick: () -> Unit,
    onTrabajadoresClick: () -> Unit = {},
    onEmpleosClick: () -> Unit = {},
    onQrClick: () -> Unit = {},
    onCentroEmpresasClick: () -> Unit = {}
) {
    val context = LocalContext.current

    var perfil by remember {
        mutableStateOf<PerfilUsuario?>(null)
    }

    var cargando by remember {
        mutableStateOf(true)
    }

    var error by remember {
        mutableStateOf("")
    }

    LaunchedEffect(Unit) {
        val resultado = PerfilCargador.cargar(context)

        resultado.onSuccess {
            perfil = it
        }

        resultado.onFailure {
            Log.e("FixNear", "No se pudo cargar el perfil", it)
            error = it.message ?: "No se pudieron cargar tus datos"
        }

        cargando = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoPrincipal)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(35.dp))

            Text(
                text = "ClickWork",
                color = TextoPrincipal,
                fontSize = 34.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Servicios, trabajo y confianza cerca de ti",
                color = TextoSecundario
            )

            Spacer(modifier = Modifier.height(24.dp))

            UsuarioCard(
                nombre = when {
                    cargando -> "Cargando..."
                    perfil != null -> perfil!!.nombre
                    else -> "Usuario"
                },
                tipoUsuario = when {
                    perfil != null -> perfil!!.tipoUsuario.ifBlank { "Cliente" }
                    error.isNotBlank() -> "No se pudieron cargar tus datos"
                    else -> ""
                },
                onClick = onPerfilClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            MenuButton(
                titulo = "Buscar Servicios",
                icono = Icons.Default.HomeRepairService,
                onClick = onServiciosClick
            )

            MenuButton(
                titulo = "Modo Trabajador",
                icono = Icons.Default.Build,
                onClick = onTrabajadoresClick
            )

            MenuButton(
                titulo = "Empleos Cercanos",
                icono = Icons.Default.Work,
                onClick = onEmpleosClick
            )

            MenuButton(
                titulo = "Centro de empresas · Publicar vacante",
                icono = Icons.Default.Business,
                onClick = onCentroEmpresasClick
            )

            MenuButton(
                titulo = "Escanear QR",
                icono = Icons.Default.QrCodeScanner,
                onClick = onQrClick
            )
        }
    }
}

@Composable
fun UsuarioCard(
    nombre: String,
    tipoUsuario: String,
    onClick: () -> Unit,
    verificado: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Usuario",
                tint = MoradoPrincipal,
                modifier = Modifier.size(58.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = nombre,
                    color = TextoPrincipal,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = tipoUsuario,
                    color = TextoSecundario,
                    fontSize = 14.sp
                )

                Text(
                    text = "Toca para ver tu perfil",
                    color = AzulAcento,
                    fontSize = 13.sp
                )
            }

            // Solo se muestra cuando la cuenta realmente está verificada
            if (verificado) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = "Verificado",
                    tint = AzulAcento,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun MenuButton(
    titulo: String,
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MoradoPrincipal
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = titulo,
                color = TextoPrincipal,
                fontSize = 18.sp
            )
        }
    }
}