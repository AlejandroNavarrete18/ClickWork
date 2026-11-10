# Pruebas automáticas - FixNear

## Descripción

En el proyecto FixNear se configuraron pruebas automáticas utilizando Gradle y GitHub Actions.

El objetivo de estas pruebas es validar que el proyecto compile correctamente y que las pruebas unitarias configuradas se ejecuten sin errores cada vez que se suben cambios al repositorio.

## Herramientas utilizadas

- Android Studio
- Gradle
- GitHub Actions
- Kotlin
- Jetpack Compose

## Comando utilizado

El pipeline ejecuta el siguiente comando:

```bash
./gradlew testDebugUnitTest