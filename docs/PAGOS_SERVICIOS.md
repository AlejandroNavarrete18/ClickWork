# Pagos de servicios (cliente → trabajador)

## Métodos disponibles (al tocar **Pagar**)
| Método | Cómo se procesa | Quién lo marca como pagado |
|---|---|---|
| Tarjeta crédito/débito, SPEI, OXXO | En línea, página de Mercado Pago (Checkout Pro). Los métodos que veas dependen de lo habilitado en tu cuenta de Mercado Pago. | Solo el webhook del servidor → `aprobado` |
| Efectivo | La app solo deja constancia | Cliente avisa → `por_confirmar`; el trabajador confirma → `confirmado`. Si el trabajador lo registra, queda `confirmado` directo |
| Transferencia directa | Igual que efectivo (los datos bancarios se piden por el chat) | Igual que efectivo |

SQL en orden: `20261114_pagos_servicio_mercadopago.sql` y luego `20261115_pagos_metodos.sql`.
Efectivo y transferencia directa **no mueven ni garantizan dinero**: son constancia entre las partes.
Los pagos de OXXO/SPEI pueden tardar; el enlace de pago dura 72 h y el estado cambia cuando el webhook avisa.

# Detalle del pago en línea con Mercado Pago

## Cómo funciona
1. El cliente toca **Pagar con Mercado Pago** en una solicitud `Confirmada`, `En curso` o `Completada`.
2. La app llama a la Edge Function `crear-pago-servicio`. El servidor valida que sea el cliente de esa solicitud, toma el monto de `solicitudes.precio_cotizado`, crea un registro `pendiente` en `pagos_servicio` y una preferencia de Checkout Pro. La app solo recibe la URL y la abre en el navegador.
3. El cliente paga en Mercado Pago (la app nunca ve datos de tarjeta).
4. Mercado Pago avisa a `mercadopago-webhook`. La función verifica la firma, consulta el pago directo a la API de Mercado Pago, compara monto y moneda y recién entonces marca `aprobado`. También notifica al trabajador.
5. Al volver a la app, la lista se refresca y muestra "Pagado".

El celular nunca marca nada como pagado: `pagos_servicio` no tiene políticas de escritura para usuarios.

## Pasos de despliegue
1. Respaldar Supabase y trabajar primero en un proyecto de desarrollo.
2. Ejecutar `supabase/migrations/20261114_pagos_servicio_mercadopago.sql`.
3. En Mercado Pago Developers crea una aplicación (Checkout Pro) y obtén:
   - Access Token (empieza con credenciales de **prueba**).
   - Clave secreta de webhooks (Tus integraciones → Webhooks).
4. Secretos y despliegue:
   ```
   supabase secrets set MP_ACCESS_TOKEN=... MP_WEBHOOK_SECRET=...
   supabase functions deploy crear-pago-servicio
   supabase functions deploy mercadopago-webhook --no-verify-jwt
   ```
5. En Mercado Pago → Webhooks, configura la URL
   `https://<tu-proyecto>.supabase.co/functions/v1/mercadopago-webhook` con el evento **Pagos**.
6. (Opcional) `supabase secrets set MP_RETORNO_URL=https://tu-pagina` para que Mercado Pago regrese al cliente a una página tuya. Sin esto el cliente vuelve a la app manualmente y la lista se refresca sola.

## Pruebas obligatorias (modo prueba, antes de producción)
- Con cuentas de prueba de Mercado Pago (vendedor y comprador) paga con tarjetas de prueba: aprobada, rechazada y pendiente.
- Confirmar que `pagos_servicio.estado` cambia **solo** después del webhook.
- Enviar al webhook una petición con firma falsa → debe responder 401.
- Intentar pagar con una cuenta que no es el cliente → 403. Intentar pagar dos veces → 409.
- Con el usuario cliente, `insert/update` en `pagos_servicio` desde el cliente REST → denegado.
- Cancelar una solicitud con pago aprobado → error pidiendo contactar a soporte.

## Decisiones pendientes (importante antes de salir a producción)
- **¿A quién llega el dinero?** Con esta implementación el cobro entra a la cuenta de Mercado Pago **de ClickWork**, no a la del trabajador. Hay dos caminos: (a) tú liberas el pago al trabajador manualmente/por transferencia (más simple para arrancar), o (b) usar Mercado Pago Marketplace con cuentas vinculadas por OAuth y comisión (`marketplace_fee`) para dividir el pago automáticamente. (b) requiere que cada trabajador conecte su cuenta y es un cambio mayor.
- **Reembolsos y disputas:** no hay botón en la app; se hacen desde el panel de Mercado Pago y el webhook marca `reembolsado`. Un doble pago accidental queda en `en_revision`.
- **Comisión de ClickWork, facturación e impuestos:** definir con un contador antes de cobrar de verdad.
- Esta entrega **no está probada contra Mercado Pago**: se escribió sin acceso a red ni a tu proyecto. Hay que probarla en sandbox.

## Orden rápido de activación
1. Supabase → SQL Editor: ejecutar `supabase/pagos_todo_en_uno.sql` (una vez). Con esto ya funcionan efectivo y transferencia directa.
2. Para pagos en línea (tarjeta/SPEI/OXXO):
   - Secret `MP_ACCESS_TOKEN` (Access Token de Mercado Pago, de pruebas primero).
   - Desplegar `crear-pago-servicio` y `mercadopago-webhook` (esta última SIN verificar JWT).
   - Registrar en Mercado Pago el webhook `https://<proyecto>.supabase.co/functions/v1/mercadopago-webhook` (evento Pagos) y guardar su clave como secret `MP_WEBHOOK_SECRET`.
3. Error "Requested function was not found" en la app = la función `crear-pago-servicio` aún no está desplegada.

## Chat por solicitud
Requiere la migración `20261113_zip_b_marketplace.sql` (tablas `conversaciones` y `mensajes`). Botón "Chatear con..." en cada solicitud no cancelada ni rechazada; se actualiza cada 4 s con la pantalla abierta (sin notificaciones push).
