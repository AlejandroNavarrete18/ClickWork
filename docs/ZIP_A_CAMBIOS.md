# ClickWork · ZIP A — Marketplace de servicios (base segura)

> **Estado de verificación:** este ZIP se preparó sin poder compilar con Gradle ni
> ejecutar el SQL contra tu Supabase. El código se revisó a mano (balance de llaves,
> firmas, referencias), pero **debes compilar y probar** siguiendo la lista del final.

## Decisiones de producto aplicadas
- El nombre visible sigue siendo **ClickWork**. Paquete `com.example.fixnearv1` y
  esquema `fixnear://` se dejan sin cambios (el esquema está ligado al login con
  Google y a la configuración de redirección de Supabase).
- **Todos los usuarios** buscan empleo, publican vacantes (como particular) y solicitan
  servicios. **Ofrecer servicios es opcional** y se activa en *Perfil → Modo trabajador*.
  Ya no se elige "Cliente / Trabajador" al registrarse.

## Orden de instalación
1. Respalda Supabase y prueba en un proyecto/rama de desarrollo.
2. Ejecuta `select distinct estado from public.solicitudes;` y confirma que solo hay
   `Pendiente` / `Aceptada` (u otros valores de la lista del SQL).
3. Ejecuta `supabase/migrations/20261110_zip_a_marketplace.sql` completo (una vez).
4. Abre la carpeta en Android Studio, sincroniza Gradle y ejecuta.

## Qué cambia
| Área | Cambio |
|---|---|
| Seguridad | Se elimina la política que dejaba leer correo y teléfono de todos los trabajadores. La app lee `perfiles_publicos` (sin contacto, coordenadas redondeadas a ~1 km). |
| Solicitudes | Estados: Pendiente → Cotizada → Confirmada → En curso → Completada (+ Rechazada / Cancelada). Solo cambian mediante funciones del servidor. Los teléfonos se comparten al confirmar. |
| Cliente | Escribe **qué necesita** al solicitar (antes se enviaba la descripción del trabajador). Pantalla **Mis solicitudes**: confirmar, cancelar, reprogramar, completar. |
| Trabajador | Pantalla **Modo trabajador** (activar, disponibilidad, oficio, descripción, ubicación actual). Recibe y cotiza en **Mis solicitudes → Recibidos**. |
| Ubicación | Los perfiles nuevos ya **no** reciben coordenadas inventadas (24.8091, -107.3940). Sin ubicación real el trabajador no aparece en el mapa. |
| Limpieza | Se eliminó `TrabajadorHomeScreen` (no estaba enlazada desde la navegación nueva y dependía de actualizaciones directas que ahora el servidor bloquea), `FixNearMap.kt` y la dependencia OSMDroid. |

## Problemas que se encontraron y se corrigieron
1. Los trabajadores no podían ver sus solicitudes: `TrabajadorHomeScreen` quedó huérfana al rediseñar la navegación.
2. Todos los perfiles se creaban en el mismo punto exacto del mapa.
3. El cliente podía insertar una solicitud directamente en estado "Completada".
4. La solicitud enviaba la descripción del trabajador en lugar de la del cliente.

## Límites conocidos (próximas entregas)
- "Iniciar servicio" lo pulsa el trabajador y "Completar" el cliente. Es provisional: el **código de encuentro** (ZIP D) evitará servicios simulados entre cuentas aliadas.
- Los teléfonos de una solicitud ya confirmada siguen guardados en la fila de la solicitud (solo las dos partes pueden leerla). El chat (ZIP B) permitirá dejar de compartirlos.
- Los usuarios ya registrados como "Trabajador" pasan automáticamente a `ofrece_servicios = true`, pero si no tienen ubicación real seguirán sin aparecer en el mapa hasta guardarla en Modo trabajador.
- Pendiente: categorías, servicios ofrecidos con precio, disponibilidad semanal, chat, reseñas y notificaciones (ZIP B).

## Lista de pruebas (3 cuentas: A cliente, B trabajador, C ajena)
1. B activa Modo trabajador, guarda ubicación → aparece en Servicios y en el mapa.
2. A solicita a B con una descripción → estado Pendiente. C **no** ve esa solicitud.
3. B cotiza → A ve el precio y confirma → ambos ven el teléfono del otro.
4. A intenta cancelar y reprogramar; B acepta la nueva fecha. B no puede aceptar su propia propuesta.
5. B inicia, A completa. A no puede completar antes de que B inicie.
6. Con la sesión de A, `GET /rest/v1/perfiles?select=correo,telefono` **no** devuelve datos de B.
7. Registro nuevo: ya no pide tipo de usuario y el perfil nace sin coordenadas.
