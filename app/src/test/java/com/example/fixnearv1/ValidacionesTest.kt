package com.example.fixnearv1

import com.example.fixnearv1.utils.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionesTest {

    @Test
    fun correo_valido_acepta_formatos_normales() {
        assertTrue(Validaciones.correoValido("m.uribe23@info.uas.edu.mx"))
        assertTrue(Validaciones.correoValido("  persona@correo.com  "))
    }

    @Test
    fun correo_invalido_se_rechaza() {
        assertFalse(Validaciones.correoValido(""))
        assertFalse(Validaciones.correoValido("sin-arroba.com"))
        assertFalse(Validaciones.correoValido("a@b"))
        assertFalse(Validaciones.correoValido("a b@correo.com"))
    }

    @Test
    fun telefono_se_normaliza_a_10_digitos() {
        assertEquals("6671234567", Validaciones.normalizarTelefonoMx("667 123 4567"))
        assertEquals("6671234567", Validaciones.normalizarTelefonoMx("+52 667-123-4567"))
        assertEquals("6671234567", Validaciones.normalizarTelefonoMx("+521 667 123 4567"))
    }

    @Test
    fun telefono_valido_solo_con_10_digitos() {
        assertTrue(Validaciones.telefonoValido("667 123 4567"))
        assertFalse(Validaciones.telefonoValido("12345"))
        assertFalse(Validaciones.telefonoValido(""))
    }

    @Test
    fun password_exige_8_caracteres_letra_y_numero() {
        assertTrue(Validaciones.passwordValida("Clave1234"))
        assertFalse(Validaciones.passwordValida("corta1"))
        assertFalse(Validaciones.passwordValida("sololetrasaqui"))
        assertFalse(Validaciones.passwordValida("1234567890"))
    }
}
