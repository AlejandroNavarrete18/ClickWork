# ClickWork Android

Aplicación móvil de oportunidades laborales y contratación de servicios, construida con Kotlin, Jetpack Compose, Supabase y OpenStreetMap.

## Experiencia principal
- Inicio con accesos directos y vacantes recientes.
- Navegación inferior: Inicio, Empleos, Servicios, Mapa y Perfil.
- Mapa OpenStreetMap con ubicación del dispositivo bajo permiso y vacantes con coordenadas reales.
- Autenticación Supabase, postulaciones, solicitudes de servicio, perfiles y QR heredados del proyecto base.

La migración de base de datos del ZIP B está en `supabase/migrations/20261113_zip_b_marketplace.sql`; consulta `supabase/ZIP_B_MERCADO_SERVICIOS.md` para su alcance y pruebas. El chat por solicitud y los pagos de servicios ya tienen interfaz Android (ver docs/PAGOS_SERVICIOS.md); reseñas y notificaciones push siguen pendientes.\n\nConsulta `docs/DISENO_V3_PENDIENTES.md` (rediseño de Perfil, Mis solicitudes, Mapa, Modo trabajador, Login y Registro; incluye una migración de endurecimiento de postulaciones) y `docs/ZIP_A_CAMBIOS.md` (última entrega: solicitudes con estados, Modo trabajador y perfiles públicos seguros; incluye la migración de Supabase que debes ejecutar) y `docs/RECONSTRUCCION_CLICKWORK.md` para cambios, alcance y limitaciones actuales.

## Requisitos
- Android Studio reciente
- JDK 17
- Android SDK 35
- Conexión a internet para Supabase y mosaicos OpenStreetMap

## Ejecución
Abre esta carpeta en Android Studio, sincroniza Gradle y ejecuta la configuración `app`.
