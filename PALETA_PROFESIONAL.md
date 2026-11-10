# ClickWork — nueva identidad visual

Se actualizó el tema general a una apariencia clara, profesional y corporativa.

## Paleta
- Azul corporativo `#2457A6`: acciones principales, navegación y enlaces.
- Azul profundo `#1D4E89`: identidad y fondos destacados.
- Verde teal `#0F9D8A`: acento secundario y elementos positivos.
- Fondo `#F4F7FB`: fondo de las pantallas.
- Superficie `#FFFFFF`: tarjetas y campos.
- Texto principal `#172B4D`: títulos y contenido.
- Texto secundario `#475569`: descripciones.
- Ámbar `#D97706`: advertencias y elementos destacados.
- Rojo `#DC4C4C`: errores y acciones destructivas.

## Archivos actualizados
- `modelo/ui/theme/Color.kt` y `Theme.kt` (paleta Material 3 clara).
- `DisenoComun.kt` conserva los componentes existentes y ahora usa los nuevos tokens de color.
- Campos de texto, navegación inferior, menú, recuperación de contraseña, verificación de correo, perfil de trabajador y pantalla de éxito.
- Colores nativos de ventana y barras del sistema.

Se conservaron los nombres de las constantes de color existentes para evitar tener que reescribir todas las pantallas y reducir el riesgo de afectar su funcionamiento.

## Verificación
No fue posible compilar en el entorno de preparación: Gradle Wrapper intentó descargar Gradle 8.9 y el entorno no tiene conexión a `services.gradle.org`. Abre el proyecto en Android Studio y ejecuta **Build > Rebuild Project** para validar en tu equipo.
