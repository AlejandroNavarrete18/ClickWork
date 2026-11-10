# Diseño V3 — pantallas pendientes (Perfil, Mis solicitudes, Mapa, Modo trabajador, Login y Registro)

> **Estado de verificación:** esta entrega se preparó sin Gradle ni acceso a tu Supabase.
> Se comprobó a mano el balance de llaves/paréntesis/corchetes, que cada color y componente
> usado exista y que no haya nombres duplicados en el paquete. **No se compiló ni se ejecutó nada**:
> compila (`./gradlew assembleDebug`), corre las pruebas (`./gradlew testDebugUnitTest`) y prueba en un dispositivo.

## Qué cambió
| Pantalla | Cambios visibles | Lógica |
|---|---|---|
| **Mis solicitudes** | Cabecera común, selector Pedidos/Recibidos con globo ámbar de "por atender" (cotizadas que debes confirmar / pendientes que debes cotizar), línea de progreso Pendiente → Completada, avatar de la contraparte, datos con icono, botones de peligro en rojo, diálogos con el estilo de la app, estado vacío por pestaña y botón **Reintentar** si falla la carga. | Igual (mismas llamadas a `SupabaseApi`). |
| **Modo trabajador** | Tarjeta héroe con degradado (activo / apagado / disponible / oculto), interruptores dentro de tarjetas, oficios como chips que se acomodan en varias líneas, aviso de ubicación (guardada / faltante) y recordatorio cuando hay ubicación nueva sin guardar. | Igual. |
| **Perfil** | Héroe con avatar de inicial y correo, resumen **real** (Pedidos / Recibidos / Completados), grupos *Datos personales*, *Trabajo y servicios* y *Seguridad*, nueva fila **Mis solicitudes**, funciones futuras marcadas "Pronto" (ya no abren un diálogo vacío). | Mismos diálogos y llamadas de editar nombre, apellido, correo y contraseña. |
| **Mapa** | Cabecera común con contador, filtros, mapa en tarjeta redondeada, tarjeta de selección sobre el mapa con botón **Ver vacante** / **Ver perfil**. | La selección ahora se hace por **id de marcador** (antes por título: dos vacantes con el mismo puesto se confundían). Se conserva la atribución © OpenStreetMap · CARTO. |
| **Login** | Mismo fondo, formulario en tarjeta, colores de la paleta V2 (antes usaba un azul distinto), scroll y `imePadding` (el teclado ya no tapa los campos), tecla "Listo" inicia sesión, correo con teclado de correo. | Igual (auto-login, Google, recordar sesión). |
| **Registro** | Cabecera común, formulario en tarjeta, requisitos de contraseña en vivo (8 caracteres, letra, número, coinciden), campo de confirmación marcado en rojo si no coincide. | Igual; el tipo de cuenta sigue siendo "Cliente" (ofrecer servicios se activa en Modo trabajador). |

## Componentes nuevos en `iuu/components/DisenoComun.kt`
`AvisoCW` (+ `TipoAviso`), `BotonCW`, `BotonSecundarioCW`, `CampoIconoCW` (con ojo para contraseñas), `FilaAjuste`, `TarjetaAjustes`, `DivisorCW`, `DialogoCW`.

## Cosas que se quitaron a propósito
- **Perfil**: la calificación "4.97", el "4 servicios solicitados" y la insignia "No verificada" eran valores fijos (no venían de la base de datos). Se quitaron; las reseñas llegan con el ZIP B y el estado de verificación requiere leer `email_confirmed_at`.
- **Login**: el botón de Facebook no hacía nada (`/* pendiente */`). Quedó solo Google.
- **Registro**: `UserTypeCard` y `CustomOutlinedTextField` (no se usaban en ninguna otra pantalla).
- **Mapa**: la etiqueta "EN VIVO" (los datos no se actualizan en tiempo real; ahora muestra "Actualizando" / "N ubicados").

## Hallazgos de seguridad y datos (revisión de `20261111_vacantes_pro.sql`)
Se agregó `supabase/migrations/20261112_postulaciones_endurecimiento.sql`:
1. **El postulante podía crear su postulación ya como "Contratada"** llamando directo a la API REST: la política de insert solo exigía `usuario_id = auth.uid()`. La nueva política solo acepta `estado = 'Pendiente'` y vacantes disponibles.
2. **Falta un índice único `(vacante_id, usuario_id)`** en las migraciones del repositorio; la app inserta con `on_conflict=vacante_id,usuario_id`, que lo necesita. El bloque lo crea solo si no hay duplicados.
3. Las consultas de verificación (CHECK de `estado`, tipos `uuid`) vienen al inicio del archivo, igual que pediste en la entrega anterior.

Si algún día vuelves a ejecutar `supabase/politicas_rls.sql`, reemplaza la política de insert por la débil: vuelve a correr la migración 20261112.

## Lista de pruebas manuales
1. **Registro**: las pistas de contraseña cambian a verde; con contraseñas distintas el campo se pone rojo; crea la cuenta y pasa a verificación.
2. **Login**: con teclado abierto el formulario se desplaza; "Listo" en la contraseña inicia sesión; Google abre el navegador.
3. **Perfil**: se ven tus datos; el resumen aparece (o se oculta si falla); editar nombre/apellido/correo/contraseña funciona; "Mis solicitudes" abre la pantalla y al volver regresas a Perfil.
4. **Modo trabajador**: activar, elegir oficio, usar ubicación, guardar; reabrir y comprobar que se conserva.
5. **Mis solicitudes** (con 2 cuentas): A pide servicio a B → B ve el globo ámbar en *Recibidos* → cotiza → A ve el globo en *Pedidos* → confirma → ambos ven el teléfono → B inicia → A completa. Prueba cancelar y reprogramar.
6. **Mapa**: dos vacantes con el mismo puesto → cada marcador abre la suya; "Ver vacante" abre el detalle y al volver sigues en Mapa; "Ver perfil" abre el perfil del trabajador.

## Límites conocidos
- Desde el Mapa, **Ver perfil** usa la misma navegación que Servicios: al regresar caes en la lista de Servicios (no en el mapa).
- Siguen con el diseño anterior: Servicios, Perfil de trabajador, Recuperar contraseña, Verificar correo, pantalla de éxito, Menú, QR y Splash. Los colores ya están remapeados a la paleta V2; falta pasarlos a los componentes comunes.
- El ZIP B (categorías, precios por servicio, chat, reseñas, notificaciones) sigue fuera.
