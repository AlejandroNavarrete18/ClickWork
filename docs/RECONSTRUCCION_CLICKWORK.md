# ClickWork — reconstrucción de experiencia Android

## Cambios incluidos
- Marca y tema oscuro ClickWork.
- Navegación inferior persistente: Inicio, Empleos, Servicios, Mapa y Perfil.
- Inicio nuevo con accesos directos y vacantes recientes consultadas desde Supabase.
- Mapa OpenStreetMap con permiso de ubicación y marcadores de vacantes que sí tienen latitud/longitud en Supabase.
- Se conservan los flujos existentes de autenticación, empleos, detalle, servicios, perfil, QR y centro de empresas.

## Limitaciones explícitas
- El mapa solo representa vacantes con coordenadas guardadas. No inventa ubicaciones ni trabajadores.
- Los perfiles de trabajador existentes no incluyen coordenadas en el modelo actual; por eso no se dibujan como marcadores hasta agregar ubicación geográfica y consentimiento.
- Las pantallas de IA y pagos requieren implementar y configurar los servicios backend correspondientes; no se simulan como operaciones reales.
- Se conserva la API Supabase y el esquema actual.

## Ejecutar
1. Abrir la carpeta `FixNearV2` en Android Studio.
2. Usar JDK 17 y Android SDK 35.
3. Sincronizar Gradle.
4. Ejecutar `app` en un dispositivo/emulador con internet.
5. Iniciar sesión con una cuenta existente de Supabase.
6. Conceder ubicación desde la sección Mapa para centrarlo en el dispositivo.

## Datos necesarios para el mapa
La tabla `vacantes` debe tener `latitud` y `longitud` válidas. Las vacantes sin coordenadas seguirán apareciendo en la lista de empleos, pero no en el mapa.
