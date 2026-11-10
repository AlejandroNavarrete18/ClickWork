# Mejoras de mapas de ClickWork

## Un solo motor de mapas

Las pantallas Empleos, Servicios y ClickWork Live utilizan MapLibre Native con el estilo oscuro CARTO. Se eliminó el mapa OSMDroid de Empleos y el mapa anterior de Servicios para evitar experiencias visuales y técnicas distintas.

## Empleos

- Mapa compacto con vacantes que tengan latitud y longitud.
- Marcadores azul/cian para vacantes.
- Al tocar un marcador se selecciona la vacante y se acerca la cámara.
- Se conserva la lista de vacantes y el acceso al detalle.

## Servicios

- Mapa compacto con trabajadores que tengan latitud y longitud.
- Marcadores violeta para trabajadores.
- Al tocar un marcador se abre el perfil del trabajador.
- Se conserva el flujo actual de solicitud de servicio.

## ClickWork Live

- Mantiene la vista conjunta de empleos y servicios y sus filtros.

## Consideraciones

- Los marcadores requieren coordenadas reales guardadas en Supabase.
- Si los registros no tienen coordenadas, el mapa seguirá mostrando el área inicial de Culiacán, pero no podrá inventar ubicaciones.
- No se modificaron pagos, suscripciones ni lógica financiera.
- La compilación debe verificarse en Android Studio con Gradle y dependencias disponibles; el entorno de preparación no pudo descargar Gradle desde services.gradle.org.
