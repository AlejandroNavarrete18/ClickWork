# FixNear V3 - alcance real de esta entrega

## Disponible en Android

- Se conserva el proyecto FixNearV2.
- Se agrega el acceso al Centro de empresas desde el menú principal.
- El formulario permite crear una vacante en `public.vacantes` usando el token del usuario.
- La publicación requiere la migración V3 y RLS correctamente configurado.

## Preparado, no terminado

- Tablas de empresas y miembros.
- Tablas de planes, suscripciones, pagos y eventos de webhook.
- Registro de consumo de IA y reportes.
- Estructura inicial del panel web.
- Diseño de responsabilidades para Edge Functions.

## No está activo todavía

- Procesamiento de voz o imágenes con IA.
- Generación real de CV y compatibilidad automatizada.
- Checkout y cobro recurrente real.
- Verificación documental de empresas.
- Panel administrativo conectado a datos reales.
- Funciones de escrow, retención de fondos o pago de salarios.

## Por qué se separan

No es seguro simular pagos, activar planes desde el cliente o colocar claves privadas en el APK. Las integraciones de IA y pagos deben implementarse en Edge Functions, probarse en sandbox y validarse mediante eventos firmados del proveedor.
