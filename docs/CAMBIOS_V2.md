# Cambios de la V2

> Los cambios se hicieron revisando el código, **sin poder compilar ni ejecutar la app**
> (no hay Android SDK donde se prepararon). Compila en Android Studio y prueba el checklist de abajo.

## Bugs corregidos
- **Mapa bloqueado (403):** OpenStreetMap rechazaba el User-Agent `com.example.fixnearv1`. Ahora usa `Constantes.USER_AGENT_MAPA`.
- **"No se pudieron cargar tus datos":** quien entraba con Google nunca tenía fila en `perfiles`. Ahora se crea al iniciar sesión (`SupabaseApi.asegurarPerfil`), y el menú refresca el token vencido antes de rendirse (`PerfilCargador`). El error real queda en Logcat con la etiqueta `FixNear`.
- **Callback de Google con la app abierta:** el deep link solo se procesaba al arrancar. `MainActivity.pendingIntent` ahora es un estado de Compose y la actividad es `singleTask`.
- **Ícono de "verificado" siempre visible:** ahora solo aparece si la cuenta realmente está verificada (por ahora, nunca).

## Seguridad
- Sesión guardada con `EncryptedSharedPreferences` (antes, texto plano). Se borran los tokens viejos; cada usuario inicia sesión una vez más.
- `allowBackup="false"` y reglas de backup que excluyen preferencias y bases de datos.
- El deep link de OAuth ya no confía en el `user_id` ni en el JWT del link: el servidor valida el token (`/auth/v1/user`).
- Registro: correo, teléfono (10 dígitos) y contraseña (8+, letras y números) validados (`Validaciones`).
- Timeouts de red en OkHttp.
- `supabase/politicas_rls.sql`: plantilla de RLS (probar primero en un proyecto de prueba).

## Código eliminado (23 archivos, nadie los usaba)
Todo el bloque de inventario de otra app: Room (`AppBaseDatos`, 4 DAOs, 4 entidades, 3 repositorios),
ViewModels y fábricas, `ProductosScreen`, `ProductoCard`, `ReportesScreen`, `QrScreen` (el activo es `QrScannerScreen`),
`TrabajoresScreen` (datos de ejemplo) y la prueba `2 + 2`.
También se quitaron Room y KSP de Gradle y las dependencias duplicadas.

## Pruebas y CI
- Pruebas reales: `ValidacionesTest`, `PerfilCargadorTest`.
- CI: caché de Gradle, Android Lint (informativo), escaneo de secretos con gitleaks, APK y reportes como artefactos, Dependabot.

## Checklist de prueba manual
- [ ] Registro con correo (cliente y trabajador) y luego login
- [ ] Login con Google en usuario nuevo → el menú muestra su nombre
- [ ] Cerrar y abrir la app con "recordar sesión"
- [ ] Mapa: se ven los mosaicos (sin "Access blocked")
- [ ] Buscar servicios, crear y aceptar una solicitud
- [ ] Empleos: ver vacante y postularse
- [ ] Rotar la pantalla en el menú

## Pendiente (no se hizo)
- PKCE para Google OAuth (hoy sigue el flujo implícito)
- Refresco automático del token en TODAS las pantallas (hoy solo menú y login)
- `crearPerfil` guarda a todos en la misma coordenada (centro de Culiacán): falta GPS real
- Cambiar la navegación por `NavHost` y mover la lógica de pantallas a ViewModels
- Partir `SupabaseApi.kt` (~37 KB) en repositorios
- Renombrar el paquete `iuu` → `ui` y decidir el nombre final (ClickWork o FixNear)
- `isMinifyEnabled = true` en release (con pruebas)
- Proveedor de tiles propio antes de publicar (OSM no es para apps con muchos usuarios)
