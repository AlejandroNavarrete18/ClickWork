# Vacantes PRO — formas de pago, documentos y CV

## Qué se agregó
**Publicar vacante** (asistente de 4 pasos con vista previa):
1. Puesto: nombre, negocio, categoría, plazas, "urgente".
2. Pago: sueldo, periodo de pago (por hora…comisión), formas de pago (efectivo, transferencia, depósito, billetera digital, cheque), prestaciones.
3. Detalles: jornada, modalidad, horario, descripción, requisitos.
4. Postulación: documentos que se piden (CV, carta, ID, domicilio, portafolio), permitir adjuntar CV, contacto, dirección.

**Mis vacantes**: ver activas/cerradas, cerrar/reabrir, ver postulantes (mensaje, sueldo esperado, CV, llamar) y marcarlos Preseleccionada / Contratada / Rechazada.

**Empleos**: búsqueda, filtros (Urgentes, Acepta CV, Remoto, Pago en efectivo + categorías), mapa opcional, tarjetas con sueldo/periodo/pago/jornada.

**Detalle de vacante**: sueldo, horario, plazas, formas de pago, documentos requeridos y hoja de postulación con mensaje, sueldo esperado y CV (PDF/Word, máx. 5 MB).

## Seguridad
- CVs en bucket privado `cvs` (carpeta = id del usuario). Solo el dueño del CV y el empleador de esa vacante pueden leerlo (URL firmada de 10 min).
- El estado de la postulación solo lo cambia el empleador, mediante `cambiar_estado_postulacion` (security definer).
- Aplicar a la misma vacante dos veces se ignora (ya existía): si alguien aplicó sin CV, no puede añadirlo después.

## Antes de ejecutar `supabase/migrations/20261111_vacantes_pro.sql`
1. Dev/rama + respaldo.
2. Revisa si `postulaciones.estado` tiene CHECK (consulta al inicio del SQL) y agrega los estados nuevos.
3. Confirma que `postulaciones.vacante_id` y `vacantes.id` son del mismo tipo (uuid).

## Pruebas con 2–3 cuentas
- A publica con pago y documentos → B lo ve en Empleos con filtros y en Detalle.
- B aplica con CV → A ve a B, abre el CV y cambia su estado.
- C (sin relación) no puede abrir el CV de B.
- A cierra la vacante → desaparece de Empleos pero sigue en Mis vacantes.
