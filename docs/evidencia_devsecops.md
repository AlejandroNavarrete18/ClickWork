# Evidencia DevSecOps - FixNear

## 1. Repositorio de código fuente

El proyecto FixNear fue subido a GitHub para utilizar control de versiones mediante Git.

Repositorio:
https://github.com/AlejandroNavarrete18/FixNear

El repositorio contiene el código fuente de la aplicación móvil desarrollada en Android Studio con Jetpack Compose.

## 2. Control de versiones

Se utilizó Git para registrar los avances del proyecto mediante commits.

Algunos avances registrados fueron:

- Creación del prototipo visual.
- Implementación de navegación entre pantallas.
- Agregado de pantallas de login, registro y verificación.
- Agregado de servicios, empleos, perfil y modo trabajador.
- Implementación del pipeline CI/CD.

## 3. Pipeline CI/CD

Se creó un workflow en GitHub Actions mediante el archivo:

.github/workflows/android-ci.yml

Este pipeline realiza las siguientes acciones:

- Descarga el código fuente.
- Configura el JDK.
- Da permisos de ejecución a Gradle.
- Compila el proyecto Android.
- Ejecuta pruebas unitarias.

## 4. Pruebas automáticas

El pipeline ejecuta pruebas unitarias con Gradle mediante el comando:

./gradlew testDebugUnitTest

El resultado del pipeline fue exitoso, lo que indica que el proyecto compila correctamente y las pruebas configuradas se ejecutan sin fallos.

## 5. Despliegue IaC

El archivo android-ci.yml representa una forma de Infraestructura como Código, ya que define mediante código el entorno necesario para compilar y probar la aplicación.

Este archivo configura:

- Sistema operativo del runner.
- Versión de Java.
- Permisos de Gradle.
- Comandos de compilación.
- Comandos de pruebas.

## 6. Monitoreo

El monitoreo se realiza desde la pestaña Actions de GitHub, donde se puede observar:

- Estado del pipeline.
- Tiempo de ejecución.
- Resultado de compilación.
- Errores detectados.
- Resultado de pruebas.

En la ejecución revisada, el pipeline finalizó correctamente en estado exitoso.

## 7. Conclusión

La implementación de DevSecOps en FixNear permite automatizar la validación del proyecto, mantener control de versiones y detectar errores de forma temprana durante el desarrollo.