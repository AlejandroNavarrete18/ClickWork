# Edge Functions pendientes

Las funciones privilegiadas deben ejecutarse en Supabase Edge Functions, nunca desde el APK.

Funciones planeadas:

- `ai-create-vacancy`: transcribe audio o procesa texto y devuelve un borrador JSON. El usuario debe revisar y confirmar antes de publicar.
- `ai-create-resume`: genera un borrador de CV sin inventar experiencia ni estudios.
- `ai-match-candidates`: calcula compatibilidad con criterios explicables y sin atributos sensibles.
- `mercadopago-webhook`: valida la firma/origen del evento, consulta el recurso en Mercado Pago, registra idempotentemente el evento y actualiza la suscripción.
- `admin-actions`: verifica rol administrativo en servidor y registra cada operación en auditoría.

Antes de desplegar:

1. Configura secretos con `supabase secrets set`; nunca los escribas en Kotlin, JavaScript del navegador o archivos versionados.
2. Valida el JWT del usuario con Supabase Auth.
3. Aplica límites de uso, validación de entradas y registros de auditoría.
4. Prueba webhooks con las herramientas sandbox del proveedor.
5. No uses una respuesta de checkout o un parámetro enviado por Android para activar un plan.

Estas funciones no se despliegan automáticamente ni están activas en esta entrega.
