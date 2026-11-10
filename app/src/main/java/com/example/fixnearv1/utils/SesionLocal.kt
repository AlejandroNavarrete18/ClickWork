package com.example.fixnearv1.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Guarda la sesión en SharedPreferences CIFRADAS (AES-256, llave en Android Keystore).
 * Antes los tokens se guardaban en texto plano.
 */
object SesionLocal {

    private const val PREFS = "fixnear_sesion_segura"
    private const val PREFS_ANTERIOR = "fixnear_sesion" // texto plano (versión 1)
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_CORREO = "correo"
    private const val KEY_RECORDAR = "recordar_sesion"

    private fun prefs(context: Context): SharedPreferences {
        val app = context.applicationContext

        // Elimina los tokens en texto plano que dejó la versión anterior
        app.deleteSharedPreferences(PREFS_ANTERIOR)

        return try {
            crearCifradas(app)
        } catch (e: Exception) {
            // Si el Keystore se corrompió, empezamos limpio (el usuario inicia sesión otra vez)
            app.deleteSharedPreferences(PREFS)
            crearCifradas(app)
        }
    }

    private fun crearCifradas(app: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(app)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            app,
            PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ─── Guardar sesión completa ─────────────────────────────────────────────
    fun guardarSesion(
        context: Context,
        sesion: SesionSupabase,
        recordar: Boolean = false
    ) {
        prefs(context)
            .edit()
            .putString(KEY_USER_ID, sesion.userId)
            .putString(KEY_ACCESS_TOKEN, sesion.accessToken)
            .putString(KEY_REFRESH_TOKEN, sesion.refreshToken)
            .putString(KEY_CORREO, sesion.correo)
            .putBoolean(KEY_RECORDAR, recordar)
            .apply()
    }

    // ─── Verificar si hay sesión guardada para auto-login ────────────────────
    fun haySesionGuardada(context: Context): Boolean {
        val p = prefs(context)
        val recordar = p.getBoolean(KEY_RECORDAR, false)
        val userId = p.getString(KEY_USER_ID, "").orEmpty()
        val token = p.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        return recordar && userId.isNotBlank() && token.isNotBlank()
    }

    // ─── Obtener sesión guardada (para refrescar token si expiró) ────────────
    fun obtenerSesionGuardada(context: Context): SesionSupabase? {
        val p = prefs(context)
        val userId = p.getString(KEY_USER_ID, "").orEmpty()
        val token = p.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        val refresh = p.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        val correo = p.getString(KEY_CORREO, "").orEmpty()
        if (userId.isBlank()) return null
        return SesionSupabase(
            userId = userId,
            correo = correo,
            accessToken = token,
            refreshToken = refresh
        )
    }

    fun obtenerUserId(context: Context): String =
        prefs(context).getString(KEY_USER_ID, "").orEmpty()

    fun obtenerAccessToken(context: Context): String =
        prefs(context).getString(KEY_ACCESS_TOKEN, "").orEmpty()

    fun obtenerRefreshToken(context: Context): String =
        prefs(context).getString(KEY_REFRESH_TOKEN, "").orEmpty()

    // ─── Actualizar access token después de refrescar ────────────────────────
    fun actualizarTokens(
        context: Context,
        accessToken: String,
        refreshToken: String
    ) {
        prefs(context)
            .edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    // ─── Cerrar sesión (borra todo) ──────────────────────────────────────────
    fun cerrarSesion(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
