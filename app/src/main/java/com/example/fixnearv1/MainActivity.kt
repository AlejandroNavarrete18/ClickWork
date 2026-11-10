package com.example.fixnearv1

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.fixnearv1.navigation.AppNavigation
import com.example.fixnearv1.modelo.ui.theme.FixNearV1Theme

class MainActivity : ComponentActivity() {

    companion object {
        // Es un estado de Compose: si la app ya estaba abierta y vuelve del navegador
        // (callback de Google), la navegación reacciona al nuevo intent.
        var pendingIntent: Intent? by mutableStateOf<Intent?>(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Solo en el primer arranque; al rotar la pantalla no se vuelve a procesar el link
        if (savedInstanceState == null) {
            pendingIntent = intent
        }

        setContent {
            FixNearV1Theme(darkTheme = true, dynamicColor = false) {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntent = intent
    }
}
