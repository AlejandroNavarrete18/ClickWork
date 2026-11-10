package com.example.fixnearv1.iuu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.fixnearv1.modelo.ui.theme.*
import com.example.fixnearv1.utils.Vacante

private enum class ClickTab(val label:String) { HOME("Inicio"), JOBS("Empleos"), SERVICES("Servicios"), MAP("Mapa"), PROFILE("Perfil") }

@Composable
fun ClickWorkShell(
    onCerrarSesion: () -> Unit,
    onQr: () -> Unit,
    onCompany: () -> Unit,
    onWorkerProfile: (String) -> Unit
) {
    var tab by remember { mutableStateOf(ClickTab.HOME) }
    var selectedVacancy by remember { mutableStateOf<Vacante?>(null) }
    var showRequests by remember { mutableStateOf(false) }
    var showWorkerMode by remember { mutableStateOf(false) }
    val bg=FondoPrincipal
    Scaffold(
        containerColor=bg,
        bottomBar={
            NavigationBar(containerColor=FondoSecundario, contentColor=TextoPrincipal, tonalElevation=0.dp) {
                ClickTab.values().forEach { item ->
                    val icon=when(item) { ClickTab.HOME->Icons.Default.Home; ClickTab.JOBS->Icons.Default.Work; ClickTab.SERVICES->Icons.Default.Handyman; ClickTab.MAP->Icons.Default.Map; ClickTab.PROFILE->Icons.Default.Person }
                    NavigationBarItem(selected=tab==item, onClick={tab=item;selectedVacancy=null;showRequests=false;showWorkerMode=false}, icon={Icon(icon,null)}, label={Text(item.label)}, colors=NavigationBarItemDefaults.colors(selectedIconColor=MoradoPrincipal, selectedTextColor=MoradoPrincipal, indicatorColor=Color(0xFFDCE8F8), unselectedIconColor=TextoSuave, unselectedTextColor=TextoSuave))
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                showRequests -> MisSolicitudesScreen(onRegresar={showRequests=false})
                showWorkerMode -> ModoTrabajadorScreen(onRegresar={showWorkerMode=false})
                selectedVacancy!=null -> DetalleVacanteScreen(vacante=selectedVacancy!!, onRegresar={selectedVacancy=null})
                else -> when(tab) {
                    ClickTab.HOME -> ClickWorkHomeScreen(onJobs={tab=ClickTab.JOBS},onServices={tab=ClickTab.SERVICES},onMap={tab=ClickTab.MAP},onCompany=onCompany,onQr=onQr,onRequests={showRequests=true},onWorkerMode={showWorkerMode=true})
                    ClickTab.JOBS -> EmpleosScreen(onRegresar={tab=ClickTab.HOME},onVerDetalle={selectedVacancy=it})
                    ClickTab.SERVICES -> ServiciosScreen(onRegresar={tab=ClickTab.HOME},onVerPerfilTrabajador=onWorkerProfile)
                    ClickTab.MAP -> ClickWorkMapScreen(onVerVacante={selectedVacancy=it},onVerTrabajador=onWorkerProfile)
                    ClickTab.PROFILE -> PerfilScreen(onRegresar={tab=ClickTab.HOME},onCerrarSesion=onCerrarSesion,onModoTrabajador={showWorkerMode=true},onMisSolicitudes={showRequests=true})
                }
            }
        }
    }
}
