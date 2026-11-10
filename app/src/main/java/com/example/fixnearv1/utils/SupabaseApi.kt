package com.example.fixnearv1.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseApi {

    private const val SUPABASE_URL =
        "https://qcpyjgkmgeswumokocwu.supabase.co"

    private const val SUPABASE_KEY =
        "sb_publishable_mvuX_ps9zsgIwVopSVGcsA_k7ieRRvT"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType =
        "application/json; charset=utf-8".toMediaType()

    // ─── URL para OAuth con Google ───────────────────────────────
    fun obtenerUrlGoogleOAuth(): String {
        return "$SUPABASE_URL/auth/v1/authorize" +
                "?provider=google" +
                "&redirect_to=fixnear://auth/callback"
    }

    // ─── Intercambiar código OAuth por sesión ───────────────────
    suspend fun intercambiarCodigoOAuth(
        accessToken: String,
        refreshToken: String,
        userId: String,
        correo: String
    ): Result<SesionSupabase> {
        return Result.success(
            SesionSupabase(
                userId = userId,
                correo = correo,
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        )
    }

    // ─── Registrar usuario ──────────────────────────────────────
    suspend fun registrarUsuario(
        nombre: String,
        correo: String,
        telefono: String,
        password: String,
        tipoUsuario: String,
        oficio: String = "",
        descripcion: String = "",
        disponible: Boolean = false
    ): Result<SesionSupabase> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("email", correo)
                    .put("password", password)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/signup")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Error al registrar usuario: $responseText")
                        )
                    }

                    val json = JSONObject(responseText)

                    val user = json.optJSONObject("user")
                    val userIdRegistro = user?.optString("id").orEmpty()
                    val emailRegistro = user?.optString("email").orEmpty()

                    val session = json.optJSONObject("session")
                    var accessToken =
                        session?.optString("access_token").orEmpty()

                    val refreshToken =
                        session?.optString("refresh_token").orEmpty()

                    var sesionFinal = SesionSupabase(
                        userId = userIdRegistro,
                        correo = emailRegistro.ifBlank { correo },
                        accessToken = accessToken,
                        refreshToken = refreshToken
                    )

                    if (accessToken.isBlank() || sesionFinal.userId.isBlank()) {
                        val loginResultado = iniciarSesion(
                            correo = correo,
                            password = password
                        )

                        if (loginResultado.isFailure) {
                            return@withContext Result.failure(
                                Exception(
                                    "Cuenta creada, pero no se pudo iniciar sesión. Intenta iniciar sesión manualmente."
                                )
                            )
                        }

                        sesionFinal = loginResultado.getOrThrow()
                        accessToken = sesionFinal.accessToken
                    }

                    val perfilCreado = crearPerfil(
                        accessToken = accessToken,
                        userId = sesionFinal.userId,
                        nombre = nombre,
                        correo = sesionFinal.correo.ifBlank { correo },
                        telefono = telefono,
                        tipoUsuario = tipoUsuario,
                        oficio = oficio,
                        descripcion = descripcion,
                        disponible = disponible
                    )

                    if (perfilCreado.isFailure) {
                        return@withContext Result.failure(
                            perfilCreado.exceptionOrNull()
                                ?: Exception("No se pudo crear el perfil.")
                        )
                    }

                    Result.success(sesionFinal)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Iniciar sesión ─────────────────────────────────────────
    suspend fun iniciarSesion(
        correo: String,
        password: String
    ): Result<SesionSupabase> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("email", correo)
                    .put("password", password)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=password")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Correo o contraseña incorrectos.")
                        )
                    }

                    val json = JSONObject(responseText)

                    val accessToken = json.optString("access_token")
                    val refreshToken = json.optString("refresh_token")
                    val user = json.optJSONObject("user")

                    val userId = user?.optString("id").orEmpty()
                    val email = user?.optString("email").orEmpty()

                    if (userId.isBlank() || accessToken.isBlank()) {
                        return@withContext Result.failure(
                            Exception("No se pudo obtener la sesión.")
                        )
                    }

                    Result.success(
                        SesionSupabase(
                            userId = userId,
                            correo = email,
                            accessToken = accessToken,
                            refreshToken = refreshToken
                        )
                    )
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Refrescar sesión ───────────────────────────────────────
    suspend fun refrescarSesion(
        refreshToken: String
    ): Result<SesionSupabase> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("refresh_token", refreshToken)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/token?grant_type=refresh_token")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Sesión expirada. Inicia sesión de nuevo.")
                        )
                    }

                    val json = JSONObject(responseText)

                    val accessToken = json.optString("access_token")
                    val newRefresh = json.optString("refresh_token")
                    val user = json.optJSONObject("user")

                    val userId = user?.optString("id").orEmpty()
                    val email = user?.optString("email").orEmpty()

                    if (userId.isBlank() || accessToken.isBlank()) {
                        return@withContext Result.failure(
                            Exception("No se pudo renovar la sesión.")
                        )
                    }

                    Result.success(
                        SesionSupabase(
                            userId = userId,
                            correo = email,
                            accessToken = accessToken,
                            refreshToken = newRefresh
                        )
                    )
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Crear perfil ───────────────────────────────────────────
    private suspend fun crearPerfil(
        accessToken: String,
        userId: String,
        nombre: String,
        correo: String,
        telefono: String,
        tipoUsuario: String,
        oficio: String,
        descripcion: String,
        disponible: Boolean
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("id", userId)
                    .put("nombre", nombre)
                    .put("correo", correo)
                    .put("telefono", telefono)
                    .put("tipo_usuario", tipoUsuario)
                    .put("oficio", oficio)
                    .put("descripcion", descripcion)
                    .put("disponible", disponible)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/perfiles?on_conflict=id")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader(
                        "Prefer",
                        "resolution=merge-duplicates,return=minimal"
                    )
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Error al crear perfil: $responseText")
                        )
                    }

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Validar token con el servidor y saber quién es el usuario ──────────
    // El deep link de OAuth NO es confiable: cualquier app puede lanzarlo con datos falsos.
    suspend fun obtenerUsuarioAutenticado(
        accessToken: String
    ): Result<UsuarioAutenticado> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/user")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Sesión no válida"))
                    }

                    val json = JSONObject(responseText)
                    val id = json.optString("id")

                    if (id.isBlank()) {
                        return@withContext Result.failure(Exception("Sesión no válida"))
                    }

                    Result.success(
                        UsuarioAutenticado(
                            userId = id,
                            correo = json.optString("email")
                        )
                    )
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Crea el perfil SOLO si no existe (ej. usuarios que entran con Google) ─
    suspend fun asegurarPerfil(
        accessToken: String,
        userId: String,
        correo: String,
        nombre: String = ""
    ): Result<Unit> {
        val existente = obtenerPerfil(userId, accessToken)

        if (existente.isSuccess) return Result.success(Unit)

        val causa = existente.exceptionOrNull()

        if (causa !is PerfilNoEncontradoException) {
            return Result.failure(causa ?: Exception("No se pudo verificar el perfil"))
        }

        return crearPerfil(
            accessToken = accessToken,
            userId = userId,
            nombre = nombre.ifBlank { correo.substringBefore("@") },
            correo = correo,
            telefono = "",
            tipoUsuario = "Cliente",
            oficio = "",
            descripcion = "",
            disponible = false
        )
    }

    // ─── Obtener perfil ─────────────────────────────────────────
    suspend fun obtenerPerfil(
        userId: String,
        accessToken: String
    ): Result<PerfilUsuario> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/perfiles?id=eq.$userId&select=*")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Error al obtener perfil: $responseText")
                        )
                    }

                    val array = JSONArray(responseText)

                    if (array.length() == 0) {
                        return@withContext Result.failure(
                            PerfilNoEncontradoException()
                        )
                    }

                    val item = array.getJSONObject(0)

                    Result.success(
                        PerfilUsuario(
                            id = item.optString("id"),
                            nombre = item.optString("nombre"),
                            correo = item.optString("correo"),
                            telefono = item.optString("telefono"),
                            tipoUsuario = item.optString("tipo_usuario"),
                            oficio = item.optString("oficio"),
                            descripcion = item.optString("descripcion"),
                            disponible = item.optBoolean("disponible"),
                            latitud = obtenerDoubleNullable(item, "latitud"),
                            longitud = obtenerDoubleNullable(item, "longitud"),
                            ofreceServicios = item.optBoolean("ofrece_servicios")
                        )
                    )
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Obtener trabajadores disponibles ───────────────────────
    suspend fun obtenerTrabajadoresDisponibles(
        accessToken: String
    ): Result<List<PerfilUsuario>> {
        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "$SUPABASE_URL/rest/v1/perfiles_publicos" +
                            "?disponible=eq.true" +
                            "&select=*" +
                            "&order=nombre.asc"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Error al obtener trabajadores: $responseText")
                        )
                    }

                    val array = JSONArray(responseText)
                    val trabajadores = mutableListOf<PerfilUsuario>()

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)

                        trabajadores.add(
                            PerfilUsuario(
                                id = item.optString("id"),
                                nombre = item.optString("nombre"),
                                correo = item.optString("correo"),
                                telefono = item.optString("telefono"),
                                tipoUsuario = item.optString("tipo_usuario"),
                                oficio = item.optString("oficio"),
                                descripcion = item.optString("descripcion"),
                                disponible = item.optBoolean("disponible"),
                                latitud = obtenerDoubleNullable(item, "latitud"),
                                longitud = obtenerDoubleNullable(item, "longitud")
                            )
                        )
                    }

                    Result.success(trabajadores)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Publicar vacante (requiere migration 20261109_empleos_v3.sql) ───
    suspend fun crearVacante(
        accessToken: String,
        userId: String,
        empresa: String,
        puesto: String,
        salario: String,
        horario: String,
        descripcion: String,
        requisitos: String,
        beneficios: String,
        telefono: String,
        direccion: String,
        categoria: String = "",
        plazas: Int = 1,
        periodoPago: String = "",
        metodosPago: List<String> = emptyList(),
        documentosRequeridos: List<String> = emptyList(),
        permiteCv: Boolean = true,
        urgente: Boolean = false,
        tipoJornada: String = "",
        modalidad: String = ""
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("publicado_por", userId)
                    .put("categoria", categoria.ifBlank { JSONObject.NULL })
                    .put("plazas", plazas.coerceIn(1, 500))
                    .put("periodo_pago", periodoPago.ifBlank { JSONObject.NULL })
                    .put("metodos_pago", JSONArray(metodosPago))
                    .put("documentos_requeridos", JSONArray(documentosRequeridos))
                    .put("permite_cv", permiteCv)
                    .put("urgente", urgente)
                    .put("tipo_jornada", tipoJornada.ifBlank { JSONObject.NULL })
                    .put("modalidad", modalidad.ifBlank { JSONObject.NULL })
                    .put("empresa", empresa)
                    .put("puesto", puesto)
                    .put("salario", salario)
                    .put("horario", horario)
                    .put("descripcion", descripcion)
                    .put("requisitos", requisitos)
                    .put("beneficios", beneficios)
                    .put("telefono", telefono)
                    .put("direccion", direccion)
                    .put("disponible", true)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/vacantes")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        Result.failure(Exception("No se pudo publicar la vacante: $responseText"))
                    } else {
                        Result.success(Unit)
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Obtener vacantes disponibles ───────────────────────────
    suspend fun obtenerVacantesDisponibles(
        accessToken: String
    ): Result<List<Vacante>> {
        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "$SUPABASE_URL/rest/v1/vacantes" +
                            "?disponible=eq.true" +
                            "&select=*" +
                            "&order=created_at.desc"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Error al obtener vacantes: $responseText")
                        )
                    }

                    val array = JSONArray(responseText)
                    val vacantes = mutableListOf<Vacante>()

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)

                        vacantes.add(parseVacante(item))
                    }

                    Result.success(vacantes)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Actualizar nombre ──────────────────────────────────────
    suspend fun actualizarNombrePerfil(
        userId: String,
        accessToken: String,
        nuevoNombre: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("nombre", nuevoNombre)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/perfiles?id=eq.$userId")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val txt = response.body?.string().orEmpty()

                        return@withContext Result.failure(
                            Exception("Error al actualizar nombre: $txt")
                        )
                    }

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Actualizar correo ──────────────────────────────────────
    suspend fun actualizarCorreo(
        accessToken: String,
        userId: String,
        nuevoCorreo: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val authBody = JSONObject()
                    .put("email", nuevoCorreo)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val authRequest = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/user")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .put(authBody)
                    .build()

                client.newCall(authRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val txt = response.body?.string().orEmpty()

                        return@withContext Result.failure(
                            Exception("Error al actualizar correo: $txt")
                        )
                    }
                }

                val perfilBody = JSONObject()
                    .put("correo", nuevoCorreo)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val perfilRequest = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/perfiles?id=eq.$userId")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(perfilBody)
                    .build()

                client.newCall(perfilRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val txt = response.body?.string().orEmpty()

                        return@withContext Result.failure(
                            Exception("Error al sincronizar correo: $txt")
                        )
                    }

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Actualizar contraseña ──────────────────────────────────
    suspend fun actualizarPassword(
        accessToken: String,
        nuevaPassword: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("password", nuevaPassword)
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/auth/v1/user")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .put(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val txt = response.body?.string().orEmpty()

                        return@withContext Result.failure(
                            Exception("Error al cambiar contraseña: $txt")
                        )
                    }

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ═════════════ ZIP A · Solicitudes de servicio ═════════════
    // La lógica y los permisos viven en Supabase (funciones RPC con RLS).
    // La app solo pide la acción; el servidor decide si es válida.

    private suspend fun llamarRpc(
        funcion: String,
        accessToken: String,
        parametros: JSONObject
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/rpc/$funcion")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(parametros.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        Result.success(cuerpo)
                    } else {
                        Result.failure<String>(Exception(extraerMensajeError(cuerpo)))
                    }
                }
            } catch (e: Exception) {
                Result.failure<String>(e)
            }
        }
    }

    // PostgREST devuelve {"message": "..."} cuando una función lanza una excepción.
    private fun extraerMensajeError(cuerpo: String): String {
        return try {
            JSONObject(cuerpo).optString("message").ifBlank { cuerpo }
        } catch (e: Exception) {
            cuerpo.ifBlank { "Error de conexión. Inténtalo de nuevo." }
        }
    }

    private fun textoSeguro(item: JSONObject, clave: String): String =
        if (item.isNull(clave)) "" else item.optString(clave)

    private fun parsearSolicitud(item: JSONObject): SolicitudServicio {
        return SolicitudServicio(
            id = textoSeguro(item, "id"),
            clienteId = textoSeguro(item, "cliente_id"),
            trabajadorId = textoSeguro(item, "trabajador_id"),
            servicio = textoSeguro(item, "servicio"),
            descripcion = textoSeguro(item, "descripcion"),
            estado = textoSeguro(item, "estado"),
            latitudCliente = obtenerDoubleNullable(item, "latitud_cliente"),
            longitudCliente = obtenerDoubleNullable(item, "longitud_cliente"),
            direccionCliente = textoSeguro(item, "direccion_cliente"),
            latitudTrabajador = obtenerDoubleNullable(item, "latitud_trabajador"),
            longitudTrabajador = obtenerDoubleNullable(item, "longitud_trabajador"),
            distanciaKm = obtenerDoubleNullable(item, "distancia_km"),
            duracionMin = obtenerDoubleNullable(item, "duracion_min"),
            nombreCliente = textoSeguro(item, "nombre_cliente"),
            telefonoCliente = textoSeguro(item, "telefono_cliente"),
            nombreTrabajador = textoSeguro(item, "nombre_trabajador"),
            telefonoTrabajador = textoSeguro(item, "telefono_trabajador"),
            precioCotizado = obtenerDoubleNullable(item, "precio_cotizado"),
            fechaProgramada = textoSeguro(item, "fecha_programada"),
            fechaPropuesta = textoSeguro(item, "fecha_propuesta"),
            propuestaPor = textoSeguro(item, "fecha_propuesta_por"),
            motivoCancelacion = textoSeguro(item, "motivo_cancelacion")
        )
    }

    // ─── Crear solicitud (función crear_solicitud) ──────────────
    // El servidor toma nombres del perfil y deja los teléfonos vacíos hasta
    // que el cliente confirme. La app ya no envía datos de contacto.
    suspend fun crearSolicitud(
        accessToken: String,
        trabajadorId: String,
        servicio: String,
        descripcion: String,
        latitudCliente: Double,
        longitudCliente: Double,
        direccionCliente: String
    ): Result<Unit> {
        val parametros = JSONObject()
            .put("p_trabajador_id", trabajadorId)
            .put("p_servicio", servicio)
            .put("p_descripcion", descripcion)
            .put("p_lat", latitudCliente)
            .put("p_lng", longitudCliente)
            .put("p_direccion", direccionCliente)

        return llamarRpc("crear_solicitud", accessToken, parametros).map { }
    }

    // ─── Solicitudes donde participo (como cliente o como trabajador) ───
    suspend fun obtenerMisSolicitudes(
        accessToken: String,
        userId: String
    ): Result<List<SolicitudServicio>> {
        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "$SUPABASE_URL/rest/v1/solicitudes" +
                            "?or=(cliente_id.eq.$userId,trabajador_id.eq.$userId)" +
                            "&select=*" +
                            "&order=created_at.desc"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure<List<SolicitudServicio>>(
                            Exception(extraerMensajeError(cuerpo))
                        )
                    }

                    val array = JSONArray(cuerpo)
                    val lista = mutableListOf<SolicitudServicio>()
                    for (i in 0 until array.length()) {
                        lista.add(parsearSolicitud(array.getJSONObject(i)))
                    }
                    Result.success(lista.toList())
                }
            } catch (e: Exception) {
                Result.failure<List<SolicitudServicio>>(e)
            }
        }
    }

    suspend fun cotizarSolicitud(
        accessToken: String,
        solicitudId: String,
        precio: Double
    ): Result<Unit> {
        val parametros = JSONObject()
            .put("p_id", solicitudId)
            .put("p_precio", precio)

        return llamarRpc("cotizar_solicitud", accessToken, parametros).map { }
    }

    // Estados válidos: Rechazada, Confirmada, Cancelada, En curso, Completada.
    // El servidor comprueba quién puede hacer cada cambio.
    suspend fun cambiarEstadoSolicitud(
        accessToken: String,
        solicitudId: String,
        nuevoEstado: String,
        motivo: String? = null
    ): Result<Unit> {
        val parametros = JSONObject()
            .put("p_id", solicitudId)
            .put("p_nuevo", nuevoEstado)

        if (!motivo.isNullOrBlank()) parametros.put("p_motivo", motivo)

        return llamarRpc("cambiar_estado_solicitud", accessToken, parametros).map { }
    }

    suspend fun proponerFechaSolicitud(
        accessToken: String,
        solicitudId: String,
        fechaIso: String
    ): Result<Unit> {
        val parametros = JSONObject()
            .put("p_id", solicitudId)
            .put("p_fecha", fechaIso)

        return llamarRpc("proponer_fecha", accessToken, parametros).map { }
    }

    suspend fun responderFechaSolicitud(
        accessToken: String,
        solicitudId: String,
        acepta: Boolean
    ): Result<Unit> {
        val parametros = JSONObject()
            .put("p_id", solicitudId)
            .put("p_acepta", acepta)

        return llamarRpc("responder_fecha", accessToken, parametros).map { }
    }

    // ═════════════ Chat por solicitud ═════════════
    // Una conversación por solicitud; solo cliente y trabajador pueden leerla/escribirla (RLS en Supabase).

    /** Obtiene (o crea) la conversación de una solicitud. Devuelve su id. */
    suspend fun asegurarConversacion(accessToken: String, solicitudId: String): Result<String> {
        val parametros = JSONObject().put("p_solicitud_id", solicitudId)
        return llamarRpc("asegurar_conversacion_solicitud", accessToken, parametros)
            .map { it.trim().trim('"') }
            .mapCatching { id ->
                if (id.isBlank()) throw Exception("No se pudo abrir el chat.") else id
            }
    }

    suspend fun obtenerMensajes(accessToken: String, conversacionId: String): Result<List<MensajeChat>> {
        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "$SUPABASE_URL/rest/v1/mensajes" +
                            "?conversacion_id=eq.$conversacionId" +
                            "&select=id,remitente_id,contenido,leido_en,creado_en" +
                            "&order=creado_en.asc&limit=300"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure<List<MensajeChat>>(
                            Exception(extraerMensajeError(cuerpo))
                        )
                    }
                    val array = JSONArray(cuerpo)
                    val lista = mutableListOf<MensajeChat>()
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        lista.add(
                            MensajeChat(
                                id = textoSeguro(o, "id"),
                                remitenteId = textoSeguro(o, "remitente_id"),
                                contenido = textoSeguro(o, "contenido"),
                                leido = !o.isNull("leido_en"),
                                creadoEn = textoSeguro(o, "creado_en")
                            )
                        )
                    }
                    Result.success(lista.toList())
                }
            } catch (e: Exception) {
                Result.failure<List<MensajeChat>>(e)
            }
        }
    }

    suspend fun enviarMensaje(
        accessToken: String,
        conversacionId: String,
        remitenteId: String,
        contenido: String
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("conversacion_id", conversacionId)
                    .put("remitente_id", remitenteId)
                    .put("contenido", contenido.trim().take(3000))
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/mensajes")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()
                    if (response.isSuccessful) Result.success(Unit)
                    else Result.failure<Unit>(Exception(extraerMensajeError(cuerpo)))
                }
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
        }
    }

    suspend fun marcarMensajesLeidos(accessToken: String, conversacionId: String): Result<Unit> {
        val parametros = JSONObject().put("p_conversacion_id", conversacionId)
        return llamarRpc("marcar_mensajes_leidos", accessToken, parametros).map { }
    }

    // ═════════════ Pagos de servicios (Mercado Pago) ═════════════
    // El APK nunca maneja datos de tarjeta ni decide si algo está pagado:
    //  - pide al servidor la URL del checkout (el monto lo fija el servidor),
    //  - y solo LEE el estado del pago, que únicamente el webhook puede cambiar.

    /** Pide al servidor crear el cobro de una solicitud. Devuelve la URL del checkout de Mercado Pago. */
    suspend fun crearPagoServicio(
        accessToken: String,
        solicitudId: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("solicitud_id", solicitudId).toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/functions/v1/crear-pago-servicio")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()
                    val json = try { JSONObject(cuerpo) } catch (e: Exception) { JSONObject() }

                    if (!response.isSuccessful) {
                        val msg = json.optString("error").ifBlank { extraerMensajeError(cuerpo) }
                        return@withContext Result.failure<String>(Exception(msg))
                    }
                    val url = json.optString("url")
                    if (!url.startsWith("https://")) {
                        return@withContext Result.failure<String>(
                            Exception("No se recibió un enlace de pago válido.")
                        )
                    }
                    Result.success(url)
                }
            } catch (e: Exception) {
                Result.failure<String>(e)
            }
        }
    }

    /**
     * Pago de cada solicitud (id de solicitud -> pago).
     * Si hay varios intentos se muestra el más relevante (cobrado > en revisión > por confirmar > pendiente).
     */
    suspend fun obtenerMisPagos(
        accessToken: String,
        userId: String
    ): Result<Map<String, PagoInfo>> {
        return withContext(Dispatchers.IO) {
            try {
                val url =
                    "$SUPABASE_URL/rest/v1/pagos_servicio" +
                            "?or=(cliente_id.eq.$userId,trabajador_id.eq.$userId)" +
                            "&select=id,solicitud_id,estado,metodo" +
                            "&order=created_at.desc"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure<Map<String, PagoInfo>>(
                            Exception(extraerMensajeError(cuerpo))
                        )
                    }
                    fun prioridad(estado: String) = when (estado) {
                        "aprobado", "confirmado" -> 5
                        "en_revision" -> 4
                        "por_confirmar" -> 3
                        "pendiente" -> 2
                        "reembolsado" -> 1
                        else -> 0
                    }
                    val array = JSONArray(cuerpo)
                    val mapa = mutableMapOf<String, PagoInfo>()
                    for (i in 0 until array.length()) {
                        val o = array.getJSONObject(i)
                        val info = PagoInfo(
                            id = textoSeguro(o, "id"),
                            solicitudId = textoSeguro(o, "solicitud_id"),
                            estado = textoSeguro(o, "estado"),
                            metodo = textoSeguro(o, "metodo").ifBlank { "mercadopago" }
                        )
                        val actual = mapa[info.solicitudId]
                        // Vienen del más reciente al más antiguo: gana mayor prioridad; a igualdad, el más reciente.
                        if (actual == null || prioridad(info.estado) > prioridad(actual.estado)) {
                            mapa[info.solicitudId] = info
                        }
                    }
                    Result.success(mapa.toMap())
                }
            } catch (e: Exception) {
                Result.failure<Map<String, PagoInfo>>(e)
            }
        }
    }

    // ─── Pagos en efectivo / transferencia (solo constancia; la app no mueve dinero) ───
    // metodo: "efectivo" o "transferencia". El servidor decide el monto y el estado.
    suspend fun registrarPagoManual(
        accessToken: String,
        solicitudId: String,
        metodo: String
    ): Result<Unit> {
        val parametros = JSONObject()
            .put("p_solicitud_id", solicitudId)
            .put("p_metodo", metodo)
        return llamarRpc("registrar_pago_manual", accessToken, parametros).map { }
    }

    /** El trabajador confirma (true) o niega (false) haber recibido el pago avisado por el cliente. */
    suspend fun responderPagoManual(
        accessToken: String,
        pagoId: String,
        recibido: Boolean
    ): Result<Unit> {
        val parametros = JSONObject()
            .put("p_pago_id", pagoId)
            .put("p_recibido", recibido)
        return llamarRpc("responder_pago_manual", accessToken, parametros).map { }
    }

    /** El cliente retira su aviso de pago. */
    suspend fun cancelarPagoManual(accessToken: String, pagoId: String): Result<Unit> {
        val parametros = JSONObject().put("p_pago_id", pagoId)
        return llamarRpc("cancelar_pago_manual", accessToken, parametros).map { }
    }

    // ─── Perfil público de un trabajador (sin correo ni teléfono) ───
    suspend fun obtenerPerfilPublico(
        trabajadorId: String,
        accessToken: String
    ): Result<PerfilUsuario> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/perfiles_publicos?id=eq.$trabajadorId&select=*")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure<PerfilUsuario>(
                            Exception(extraerMensajeError(cuerpo))
                        )
                    }

                    val array = JSONArray(cuerpo)

                    if (array.length() == 0) {
                        return@withContext Result.failure<PerfilUsuario>(
                            Exception("Este trabajador ya no está disponible.")
                        )
                    }

                    val item = array.getJSONObject(0)

                    Result.success(
                        PerfilUsuario(
                            id = textoSeguro(item, "id"),
                            nombre = textoSeguro(item, "nombre"),
                            correo = "",
                            telefono = "",
                            tipoUsuario = "",
                            oficio = textoSeguro(item, "oficio"),
                            descripcion = textoSeguro(item, "descripcion"),
                            disponible = item.optBoolean("disponible"),
                            latitud = obtenerDoubleNullable(item, "latitud"),
                            longitud = obtenerDoubleNullable(item, "longitud"),
                            ofreceServicios = true
                        )
                    )
                }
            } catch (e: Exception) {
                Result.failure<PerfilUsuario>(e)
            }
        }
    }

    // ─── Modo trabajador: activar/desactivar y datos del servicio ───
    suspend fun actualizarModoTrabajador(
        accessToken: String,
        userId: String,
        ofreceServicios: Boolean,
        disponible: Boolean,
        oficio: String,
        descripcion: String,
        latitud: Double?,
        longitud: Double?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val cuerpoJson = JSONObject()
                    .put("ofrece_servicios", ofreceServicios)
                    .put("disponible", disponible)
                    .put("oficio", oficio)
                    .put("descripcion", descripcion)

                if (latitud != null && longitud != null) {
                    cuerpoJson.put("latitud", latitud)
                    cuerpoJson.put("longitud", longitud)
                }

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/perfiles?id=eq.$userId")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(cuerpoJson.toString().toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val cuerpo = response.body?.string().orEmpty()

                    if (response.isSuccessful) {
                        Result.success(Unit)
                    } else {
                        Result.failure<Unit>(Exception(extraerMensajeError(cuerpo)))
                    }
                }
            } catch (e: Exception) {
                Result.failure<Unit>(e)
            }
        }
    }

    // ─── Aplicar a una vacante ─────────────────────────────────────
    suspend fun aplicarVacante(
        accessToken: String,
        vacante: Vacante,
        perfil: PerfilUsuario,
        mensaje: String = "",
        expectativaSalarial: String = "",
        cvPath: String = "",
        cvNombre: String = ""
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject()
                    .put("vacante_id", vacante.id)
                    .put("usuario_id", perfil.id)
                    .put("nombre_usuario", perfil.nombre)
                    .put("correo_usuario", perfil.correo)
                    .put("telefono_usuario", perfil.telefono)
                    .put("empresa", vacante.empresa)
                    .put("puesto", vacante.puesto)
                    .put("estado", "Pendiente")
                    .apply {
                        if (mensaje.isNotBlank()) put("mensaje", mensaje.take(1000))
                        if (expectativaSalarial.isNotBlank()) put("expectativa_salarial", expectativaSalarial.take(80))
                        if (cvPath.isNotBlank()) {
                            put("cv_path", cvPath)
                            put("cv_nombre", cvNombre.take(120))
                        }
                    }
                    .toString()
                    .toRequestBody(jsonMediaType)

                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/postulaciones?on_conflict=vacante_id,usuario_id")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "resolution=ignore-duplicates,return=minimal")
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()

                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            Exception("Error al aplicar a la vacante: $responseText")
                        )
                    }

                    Result.success(Unit)
                }

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseVacante(item: JSONObject): Vacante {
        fun lista(campo: String): List<String> {
            val arr = item.optJSONArray(campo) ?: return emptyList()
            return (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
        }
        fun texto(campo: String): String =
            if (item.isNull(campo)) "" else item.optString(campo)

        return Vacante(
            id = item.optString("id"),
            empresa = item.optString("empresa"),
            puesto = item.optString("puesto"),
            salario = item.optString("salario"),
            horario = item.optString("horario"),
            descripcion = item.optString("descripcion"),
            requisitos = item.optString("requisitos"),
            beneficios = item.optString("beneficios"),
            telefono = item.optString("telefono"),
            direccion = item.optString("direccion"),
            latitud = obtenerDoubleNullable(item, "latitud"),
            longitud = obtenerDoubleNullable(item, "longitud"),
            disponible = item.optBoolean("disponible"),
            categoria = texto("categoria"),
            plazas = item.optInt("plazas", 1),
            periodoPago = texto("periodo_pago"),
            metodosPago = lista("metodos_pago"),
            documentosRequeridos = lista("documentos_requeridos"),
            permiteCv = if (item.has("permite_cv") && !item.isNull("permite_cv")) item.optBoolean("permite_cv") else true,
            urgente = item.optBoolean("urgente", false),
            tipoJornada = texto("tipo_jornada"),
            modalidad = texto("modalidad"),
            publicadoPor = texto("publicado_por")
        )
    }

    // ─── Mis vacantes (incluye cerradas) ───────────────────────
    suspend fun obtenerMisVacantes(accessToken: String, userId: String): Result<List<Vacante>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/vacantes?publicado_por=eq.$userId&select=*&order=created_at.desc")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val txt = response.body?.string().orEmpty()
                    if (!response.isSuccessful) return@withContext Result.failure(Exception("Error al obtener tus vacantes: $txt"))
                    val array = JSONArray(txt)
                    Result.success((0 until array.length()).map { parseVacante(array.getJSONObject(it)) })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun cambiarDisponibilidadVacante(accessToken: String, vacanteId: String, disponible: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("disponible", disponible).toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/vacantes?id=eq.$vacanteId")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Prefer", "return=minimal")
                    .patch(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(Unit)
                    else Result.failure(Exception("No se pudo actualizar la vacante: ${response.body?.string().orEmpty()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── Postulantes de una vacante (solo el dueño, por RLS) ───
    suspend fun obtenerPostulantes(accessToken: String, vacanteId: String): Result<List<Postulante>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/postulaciones?vacante_id=eq.$vacanteId&select=*&order=created_at.desc")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    val txt = response.body?.string().orEmpty()
                    if (!response.isSuccessful) return@withContext Result.failure(Exception("Error al obtener postulantes: $txt"))
                    val array = JSONArray(txt)
                    Result.success((0 until array.length()).map { i ->
                        val o = array.getJSONObject(i)
                        fun t(c: String) = if (o.isNull(c)) "" else o.optString(c)
                        Postulante(
                            id = t("id"),
                            nombre = t("nombre_usuario"),
                            correo = t("correo_usuario"),
                            telefono = t("telefono_usuario"),
                            estado = t("estado").ifBlank { "Pendiente" },
                            mensaje = t("mensaje"),
                            expectativaSalarial = t("expectativa_salarial"),
                            cvPath = t("cv_path"),
                            cvNombre = t("cv_nombre")
                        )
                    })
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun cambiarEstadoPostulacion(accessToken: String, postulacionId: String, estado: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("p_id", postulacionId).put("p_estado", estado).toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$SUPABASE_URL/rest/v1/rpc/cambiar_estado_postulacion")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(Unit)
                    else Result.failure(Exception("No se pudo cambiar el estado: ${response.body?.string().orEmpty()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // ─── CV: subir a Storage privado y obtener URL firmada ─────
    suspend fun subirCv(
        accessToken: String,
        userId: String,
        nombreArchivo: String,
        bytes: ByteArray,
        mime: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (bytes.size > 5 * 1024 * 1024) {
                    return@withContext Result.failure(Exception("El archivo supera 5 MB."))
                }
                val limpio = nombreArchivo.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(80)
                val ruta = "$userId/${System.currentTimeMillis()}_$limpio"
                val request = Request.Builder()
                    .url("$SUPABASE_URL/storage/v1/object/cvs/$ruta")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", mime)
                    .post(bytes.toRequestBody(mime.toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.success(ruta)
                    else Result.failure(Exception("No se pudo subir el CV: ${response.body?.string().orEmpty()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun urlFirmadaCv(accessToken: String, ruta: String, segundos: Int = 600): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().put("expiresIn", segundos).toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url("$SUPABASE_URL/storage/v1/object/sign/cvs/$ruta")
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build()
                client.newCall(request).execute().use { response ->
                    val txt = response.body?.string().orEmpty()
                    if (!response.isSuccessful) return@withContext Result.failure(Exception("No tienes acceso a este CV."))
                    val firmada = JSONObject(txt).optString("signedURL")
                    if (firmada.isBlank()) Result.failure(Exception("No se pudo generar el enlace."))
                    else Result.success("$SUPABASE_URL/storage/v1" + (if (firmada.startsWith("/")) firmada else "/$firmada"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun obtenerDoubleNullable(
        json: JSONObject,
        campo: String
    ): Double? {
        return if (!json.has(campo) || json.isNull(campo)) {
            null
        } else {
            json.optDouble(campo)
        }
    }
}




data class SesionSupabase(
    val userId: String,
    val correo: String,
    val accessToken: String,
    val refreshToken: String = ""
)

data class PerfilUsuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val tipoUsuario: String,
    val oficio: String,
    val descripcion: String,
    val disponible: Boolean,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val ofreceServicios: Boolean = false
)

data class SolicitudServicio(
    val id: String,
    val clienteId: String,
    val trabajadorId: String,
    val servicio: String,
    val descripcion: String,
    val estado: String,
    val latitudCliente: Double?,
    val longitudCliente: Double?,
    val direccionCliente: String,
    val latitudTrabajador: Double?,
    val longitudTrabajador: Double?,
    val distanciaKm: Double?,
    val duracionMin: Double?,
    val nombreCliente: String,
    val telefonoCliente: String,
    val nombreTrabajador: String,
    val telefonoTrabajador: String,
    val precioCotizado: Double? = null,
    val fechaProgramada: String = "",
    val fechaPropuesta: String = "",
    val propuestaPor: String = "",
    val motivoCancelacion: String = ""
)

data class PagoInfo(
    val id: String,
    val solicitudId: String,
    val estado: String,
    val metodo: String
)

data class MensajeChat(
    val id: String,
    val remitenteId: String,
    val contenido: String,
    val leido: Boolean,
    val creadoEn: String
)

data class Vacante(
    val id: String,
    val empresa: String,
    val puesto: String,
    val salario: String,
    val horario: String,
    val descripcion: String,
    val requisitos: String,
    val beneficios: String,
    val telefono: String,
    val direccion: String,
    val latitud: Double?,
    val longitud: Double?,
    val disponible: Boolean,
    val categoria: String = "",
    val plazas: Int = 1,
    val periodoPago: String = "",
    val metodosPago: List<String> = emptyList(),
    val documentosRequeridos: List<String> = emptyList(),
    val permiteCv: Boolean = true,
    val urgente: Boolean = false,
    val tipoJornada: String = "",
    val modalidad: String = "",
    val publicadoPor: String = ""
)

data class Postulante(
    val id: String,
    val nombre: String,
    val correo: String,
    val telefono: String,
    val estado: String,
    val mensaje: String,
    val expectativaSalarial: String,
    val cvPath: String,
    val cvNombre: String
)

class PerfilNoEncontradoException :
    Exception("No existe perfil para este usuario.")

data class UsuarioAutenticado(
    val userId: String,
    val correo: String
)
