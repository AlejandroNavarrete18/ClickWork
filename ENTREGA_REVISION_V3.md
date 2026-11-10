# Entrega ClickWork · revisión del ZIP V3

## Qué contiene esta carpeta
- Proyecto Android Studio original completo.
- Pantallas V3 ya presentes en el proyecto: Perfil, Mis solicitudes, Mapa, Modo trabajador, Login y Registro, además del flujo de vacantes.
- Migraciones SQL existentes del proyecto.
- Nueva migración `supabase/migrations/20261113_zip_b_marketplace.sql` para categorías, servicios/precios, disponibilidad semanal, chat por solicitud, reseñas y notificaciones.
- `supabase/ZIP_B_MERCADO_SERVICIOS.md` explica el alcance y pruebas de seguridad.

## Orden de migraciones
Revisa los pre-requisitos de cada archivo y ejecútalos en una rama/proyecto de desarrollo con respaldo. En términos generales:
1. `20261109_empleos_v3.sql`
2. `20261110_zip_a_marketplace.sql`
3. `20261111_vacantes_pro.sql`
4. `20261112_postulaciones_endurecimiento.sql`
5. `20261113_zip_b_marketplace.sql`

No ejecutes a ciegas: algunas migraciones tienen consultas previas que debes revisar contra el esquema real de tu proyecto. No tengo acceso a tu instancia Supabase ni a sus credenciales y no ejecuté SQL en tu base.

## Funciones que no deben considerarse terminadas todavía
La migración ZIP B crea el esquema y RLS, pero aún falta integrar en Kotlin las pantallas y llamadas reales de chat, reseñas, centro de notificaciones y administración de servicios/horarios. Las notificaciones push requieren además Edge Functions y credenciales del proveedor. No se agregaron botones simulados que aparenten guardar datos sin backend.

## Compilación
Se intentó ejecutar `bash gradlew assembleDebug testDebugUnitTest`, pero el entorno no pudo descargar Gradle 8.9 por falta de conexión DNS (`services.gradle.org`). Por ello no hay una compilación ni pruebas Android confirmadas. Abre el proyecto en Android Studio con JDK 17 / SDK 35, sincroniza Gradle y ejecuta las pruebas antes de producción.

## Seguridad y pagos
- No se incluyeron claves `service_role` ni secretos.
- Los CV siguen sujetos a la configuración del bucket privado y políticas de la migración de vacantes.
- Las formas de pago de vacantes siguen siendo las declaradas en el flujo existente: efectivo, transferencia, depósito, billetera digital y cheque.
- No se procesa dinero ni se simulan pagos; solo se guardan formas de pago aceptadas.
