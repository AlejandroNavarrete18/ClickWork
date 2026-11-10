package com.example.fixnearv1

import com.example.fixnearv1.iuu.screens.obtenerApellidosPerfil
import com.example.fixnearv1.iuu.screens.obtenerNombresPerfil
import org.junit.Assert.assertEquals
import org.junit.Test

class PerfilNombresTest {

    @Test
    fun una_palabra_es_solo_nombre() {
        assertEquals("Ana", obtenerNombresPerfil("Ana"))
        assertEquals("", obtenerApellidosPerfil("Ana"))
    }

    @Test
    fun dos_palabras_son_nombre_y_apellido() {
        assertEquals("Ana", obtenerNombresPerfil("Ana López"))
        assertEquals("López", obtenerApellidosPerfil("Ana López"))
    }

    @Test
    fun tres_palabras_son_dos_nombres_y_un_apellido() {
        assertEquals("Juan Carlos", obtenerNombresPerfil("Juan Carlos Pérez"))
        assertEquals("Pérez", obtenerApellidosPerfil("Juan Carlos Pérez"))
    }

    @Test
    fun cuatro_palabras_son_dos_nombres_y_dos_apellidos() {
        assertEquals("María José", obtenerNombresPerfil("María José Pérez Soto"))
        assertEquals("Pérez Soto", obtenerApellidosPerfil("María José Pérez Soto"))
    }

    @Test
    fun espacios_sobrantes_y_texto_vacio_no_rompen() {
        assertEquals("Ana", obtenerNombresPerfil("  Ana   López  "))
        assertEquals("López", obtenerApellidosPerfil("  Ana   López  "))
        assertEquals("", obtenerNombresPerfil(""))
        assertEquals("", obtenerApellidosPerfil("   "))
    }
}
