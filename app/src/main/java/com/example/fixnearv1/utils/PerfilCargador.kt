package com.example.fixnearv1.utils

import android.content.Context

/** Detecta el error que devuelve Supabase cuando el access token ya venció. */
fun mensajeIndicaTokenExpirado(mensaje: String?): Boolean {
    if (mensaje.isNullOrBlank()) return false
    return mensaje.contains("JWT expired", ignoreCase = true) ||
        mensaje.contains("PGRST303", ignoreCase = true) ||
        mensaje.contains("invalid JWT", ignoreCase = true)
}

object PerfilCargador {

    /**
     * Carga el perfil del usuario actual y se encarga de:
     *  1. Refrescar el token si venció.
     *  2. Crear el perfil si no existe (usuarios de Google).
     */
    suspend fun cargar(context: Context): Result<PerfilUsuario> {
        val userId = SesionLocal.obtenerUserId(context)
        var token = SesionLocal.obtenerAccessToken(context)

        if (userId.isBlank() || token.isBlank()) {
            return Result.failure(Exception("No hay sesión activa"))
        }

        var resultado = SupabaseApi.obtenerPerfil(userId, token)

        if (mensajeIndicaTokenExpirado(resultado.exceptionOrNull()?.message)) {
            val refresh = SesionLocal.obtenerRefreshToken(context)

            if (refresh.isNotBlank()) {
                val nueva = SupabaseApi.refrescarSesion(refresh).getOrNull()

                if (nueva != null) {
                    SesionLocal.actualizarTokens(
                        context = context,
                        accessToken = nueva.accessToken,
                        refreshToken = nueva.refreshToken
                    )
                    token = nueva.accessToken
                    resultado = SupabaseApi.obtenerPerfil(userId, token)
                }
            }
        }

        if (resultado.exceptionOrNull() is PerfilNoEncontradoException) {
            val correo = SesionLocal.obtenerSesionGuardada(context)?.correo.orEmpty()
            SupabaseApi.asegurarPerfil(
                accessToken = token,
                userId = userId,
                correo = correo
            )
            resultado = SupabaseApi.obtenerPerfil(userId, token)
        }

        return resultado
    }
}
