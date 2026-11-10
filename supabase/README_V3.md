# Supabase V3 - instalación segura

## Antes de ejecutar

1. Crea un proyecto Supabase de desarrollo o una rama de base de datos.
2. Haz respaldo de la base de datos actual.
3. Revisa que `public.vacantes` conserve las columnas usadas por la app: `empresa`, `puesto`, `salario`, `horario`, `descripcion`, `requisitos`, `beneficios`, `telefono`, `direccion`, `disponible`.
4. Ejecuta `migrations/20261109_empleos_v3.sql` en SQL Editor.
5. Verifica RLS con dos cuentas distintas: cada usuario debe poder publicar a su nombre y no modificar vacantes ajenas.

La migración es aditiva, pero debe probarse en desarrollo antes de producción. No uses la clave `service_role` en Android ni en el panel web.

## Planes iniciales

Después de revisar los precios con el equipo, se pueden cargar planes de ejemplo:

```sql
insert into public.planes (codigo, nombre, precio_mxn, vacantes_activas_max, reclutadores_max, funciones_ia)
values
 ('gratis', 'Gratis', 0, 1, 1, false),
 ('profesional', 'Profesional', 299, 5, 1, true),
 ('empresa', 'Empresa', 699, 20, 5, true)
on conflict (codigo) do nothing;
```

Estos precios son de demostración y deben validarse antes de cobrar.

## Estado de las integraciones

- Android: el formulario de Centro de empresas publica a la tabla existente `vacantes`, usando el JWT del usuario y RLS.
- Suscripciones: esquema de datos preparado; falta implementar checkout, creación/cancelación de suscripción y webhook de Mercado Pago en un entorno de pruebas.
- IA: esquema de consumo preparado; falta configurar proveedor, secretos y Edge Functions.
- Administración: el panel web es un starter; las operaciones privilegiadas requieren Edge Functions protegidas y auditoría.
- Empresas: tablas preparadas; el primer flujo Android permite publicar como particular/empleador. El alta verificada de empresa y membresías se implementa en la siguiente iteración.

No marcar una suscripción como activa desde el cliente. Solo un webhook verificado y validado contra la API del proveedor puede confirmar el estado.
