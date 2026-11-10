package com.example.fixnearv1.utils

object Validaciones {

    private val CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun correoValido(correo: String): Boolean = CORREO.matches(correo.trim())

    /** Deja solo los 10 dígitos de un teléfono mexicano (quita +52, 521, espacios y guiones). */
    fun normalizarTelefonoMx(telefono: String): String {
        var d = telefono.filter { it.isDigit() }
        if (d.length == 13 && d.startsWith("521")) d = d.drop(3)
        else if (d.length == 12 && d.startsWith("52")) d = d.drop(2)
        return d
    }

    fun telefonoValido(telefono: String): Boolean =
        normalizarTelefonoMx(telefono).length == 10

    /** Mínimo 8 caracteres, con al menos una letra y un número. */
    fun passwordValida(password: String): Boolean =
        password.length >= 8 &&
            password.any { it.isLetter() } &&
            password.any { it.isDigit() }
}
