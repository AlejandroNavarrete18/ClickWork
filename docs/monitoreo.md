# Documentación de Monitoreo - FixNear

## 1. Descripción General
El monitoreo de la Integración Continua (CI) del proyecto **FixNear** se realiza a través de **GitHub Actions**. Esta herramienta nos permite validar automáticamente la estabilidad del código base cada vez que se suben cambios al repositorio o se hacen *Pull Requests* a la rama `main`.

## 2. Elementos Monitoreados
Desde la pestaña **Actions** de GitHub, el equipo supervisa los siguientes puntos clave:
* **Estado general del pipeline:** Éxito o fallo de la integración.
* **Tiempos de ejecución:** Duración total de la descarga de dependencias, compilación y pruebas.
* **Validación de compilación:** Ejecución exitosa de la tarea de Gradle (`assembleDebug`).
* **Pruebas unitarias:** Resultados de los tests automatizados (`testDebugUnitTest`).
* **Trazabilidad:** Commits y autores responsables de cada ejecución del *workflow*.

## 3. Indicadores de Estado
GitHub Actions clasifica el estado de cada ejecución utilizando un sistema de colores sencillo:
* **Verde (Success):** La compilación fue exitosa y las pruebas pasaron. El código es seguro de integrar.
* **Amarillo (In Progress):** El servidor virtual (Ubuntu) está ejecutando los pasos actualmente.
* **Rojo (Failure):** El proceso falló. Se bloquea la integración y se requiere revisar los *logs* de la consola para identificar el error (ej. fallos de sintaxis en Kotlin o pruebas rotas).

## 4. Importancia y Resultados Observados
El monitoreo continuo es vital para detectar errores en etapas tempranas. Durante la implementación de este pipeline en FixNear, validamos que el proyecto compila correctamente utilizando la distribución de Java `temurin 17`. Las pruebas se ejecutan sin arrojar fallos y el sistema es capaz de alertar inmediatamente si una dependencia nueva rompe el proyecto.

## 5. Evidencias
Para respaldar el correcto funcionamiento del monitoreo, la documentación adjunta las siguientes capturas de pantalla:
* Historial de la pestaña Actions mostrando ejecuciones en verde.
* Desglose de los *jobs* completados (Checkout, Configuración de JDK, Permisos y Compilación).
* Logs de consola con el mensaje de salida: `BUILD SUCCESSFUL`.