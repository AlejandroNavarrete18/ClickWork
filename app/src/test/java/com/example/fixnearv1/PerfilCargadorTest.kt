package com.example.fixnearv1

import com.example.fixnearv1.utils.mensajeIndicaTokenExpirado
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfilCargadorTest {

    @Test
    fun detecta_token_expirado_de_supabase() {
        assertTrue(
            mensajeIndicaTokenExpirado(
                "Error al obtener perfil: {\"code\":\"PGRST303\",\"message\":\"JWT expired\"}"
            )
        )
        assertTrue(mensajeIndicaTokenExpirado("invalid JWT: unable to parse"))
    }

    @Test
    fun otros_errores_no_se_confunden_con_token_expirado() {
        assertFalse(mensajeIndicaTokenExpirado(null))
        assertFalse(mensajeIndicaTokenExpirado(""))
        assertFalse(mensajeIndicaTokenExpirado("No existe perfil para este usuario."))
        assertFalse(mensajeIndicaTokenExpirado("Unable to resolve host"))
    }
}
