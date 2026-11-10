# Despliegue e Infraestructura como Código (IaC) - FixNear

## 1. Descripción General
En el proyecto **FixNear**, automatizamos la preparación del entorno de compilación y pruebas utilizando prácticas de Infraestructura como Código (IaC). En lugar de configurar servidores manualmente para revisar la aplicación, definimos todo el entorno necesario mediante un archivo de configuración en **GitHub Actions**.

## 2. Infraestructura Aprovisionada
Gracias a este enfoque, cada vez que se ejecuta el pipeline, GitHub levanta y configura automáticamente la siguiente infraestructura efímera (temporal):
* **Sistema Operativo:** Un servidor virtual con la última versión de Ubuntu (`ubuntu-latest`).
* **Entorno de Desarrollo:** Instalación y configuración de Java JDK 17 (distribución `temurin`).
* **Herramientas:** Asignación de permisos de ejecución para Gradle (`gradlew`).

## 3. Ubicación del Archivo
Toda esta configuración está declarada en texto plano y guardada directamente en el repositorio. Puedes encontrar el archivo en la siguiente ruta:

```text
.github/workflows/android-ci.yml