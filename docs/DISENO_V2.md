# Rediseño visual V2 — "Noche con energía"

Paleta central en `modelo/ui/theme/Color.kt` (+ `Theme.kt` con esquema Material 3 completo).

| Uso | Color |
|---|---|
| Fondo | #0A0F1E |
| Barras / campos | #111A2E |
| Tarjetas | #131C31 |
| Chips / elevado | #1B2640 |
| Bordes | #243252 |
| Marca (botón) | #6D5BFF |
| Marca clara (iconos, links) | #B4A9FF |
| Cian | #22C7E8 |
| Éxito / disponible | #34D399 |
| Ámbar / pendiente | #FFB547 |
| Peligro | #FF6B7A |
| Héroe (degradado) | #5B3DF5 → #1FB5D6 |

Cambios: Inicio rediseñado (iconos con color propio, héroe con degradado y dos botones),
"Contratar" ahora es "Buscar servicio", barra inferior con indicador violeta sólido,
Buscar servicio con filtros por oficio, tarjeta de profesional con avatar y estado,
tema nativo oscuro (sin destello blanco al abrir). Los 173 colores fijos del resto de
pantallas se remapearon a la paleta nueva. No se tocó ninguna lógica.
