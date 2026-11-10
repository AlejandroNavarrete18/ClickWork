package com.example.fixnearv1.utils

object Constantes {

    // OpenStreetMap bloquea (403) los User-Agent genéricos como el nombre del paquete.
    // Debe identificar tu app. Si algún día lo publican, agrega un correo de contacto.
    const val USER_AGENT_MAPA = "ClickWork-FixNear/1.0 (Android; proyecto UAS Culiacan)"

    // Centro de Culiacán (se usa mientras no haya GPS del usuario)
    const val CULIACAN_LAT = 24.8091
    const val CULIACAN_LNG = -107.3940
}
