# ZIP B — Marketplace de servicios (base de datos)

Esta migración agrega el esquema seguro para completar el marketplace de servicios:
- Categorías administrables.
- Servicios/precios por trabajador.
- Disponibilidad semanal.
- Conversaciones y mensajes vinculados a una solicitud.
- Reseñas posteriores a servicios completados.
- Centro de notificaciones.

## Orden de ejecución
1. Respaldar Supabase y usar primero una rama/proyecto de desarrollo.
2. Ejecutar las migraciones anteriores en el orden descrito en `supabase/README_V3.md`.
3. Ejecutar `migrations/20261113_zip_b_marketplace.sql`.
4. Probar RLS con tres cuentas: cliente, trabajador y cuenta ajena.

## Importante: alcance real de esta entrega
Esta migración crea tablas, índices, políticas RLS y la función para obtener/crear una conversación de una solicitud. **No activa por sí sola pantallas Android de chat, reseñas ni notificaciones**, ni implementa envío push. Esas interfaces y su conexión con Supabase requieren una iteración de Kotlin adicional y pruebas con cuentas reales. No se deben presentar como funciones terminadas hasta que se integren.

## Pruebas mínimas
- Un trabajador solo puede editar sus servicios y horarios.
- Un cliente autenticado puede leer servicios activos.
- Solo el cliente y el trabajador de la solicitud pueden leer/crear la conversación y sus mensajes.
- Una reseña solo se inserta si la solicitud está `Completada`, y cada participante puede reseñar una vez.
- Cada usuario solo puede leer/marcar sus propias notificaciones.
- Una tercera cuenta no puede leer conversaciones ni CVs.

## Notas
- `dia_semana`: 0 domingo, 1 lunes, ... 6 sábado.
- Las notificaciones se insertan desde funciones/Edge Functions confiables; no se permite que una cuenta inserte notificaciones arbitrarias para otra.
- El chat se limita a solicitudes existentes; no hay chat abierto entre desconocidos.
- No colocar `service_role` en Android.
