-- ClickWork — Vacantes PRO: formas de pago, documentos solicitados, CV adjunto y gestión de postulantes.
-- Ejecutar primero en un proyecto/rama de desarrollo y con respaldo. Es aditiva (no borra datos).
-- Antes de ejecutar, revisa si postulaciones.estado tiene un CHECK que limite valores:
--   select conname, pg_get_constraintdef(oid) from pg_constraint where conrelid = 'public.postulaciones'::regclass;
-- Si existe, agrega a la lista: 'Vista','Preseleccionada','Rechazada','Contratada'.

-- ───────── Vacantes ─────────
alter table public.vacantes
  add column if not exists categoria text,
  add column if not exists plazas integer not null default 1,
  add column if not exists periodo_pago text,
  add column if not exists metodos_pago text[] not null default '{}',
  add column if not exists documentos_requeridos text[] not null default '{}',
  add column if not exists permite_cv boolean not null default true,
  add column if not exists urgente boolean not null default false;

alter table public.vacantes drop constraint if exists vacantes_plazas_chk;
alter table public.vacantes add constraint vacantes_plazas_chk check (plazas between 1 and 500);

alter table public.vacantes drop constraint if exists vacantes_periodo_pago_chk;
alter table public.vacantes add constraint vacantes_periodo_pago_chk
  check (periodo_pago is null or periodo_pago in ('Por hora','Diario','Semanal','Quincenal','Mensual','Por proyecto','Comisión'));

alter table public.vacantes drop constraint if exists vacantes_metodos_pago_chk;
alter table public.vacantes add constraint vacantes_metodos_pago_chk
  check (metodos_pago <@ array['Efectivo','Transferencia','Depósito','Billetera digital','Cheque']::text[]);

-- El dueño también ve sus vacantes cerradas (la política pública solo muestra disponibles).
drop policy if exists vacantes_select_propias on public.vacantes;
create policy vacantes_select_propias on public.vacantes for select to authenticated
using (publicado_por = auth.uid());

-- ───────── Postulaciones ─────────
alter table public.postulaciones
  add column if not exists mensaje text,
  add column if not exists expectativa_salarial text,
  add column if not exists cv_path text,
  add column if not exists cv_nombre text;

alter table public.postulaciones drop constraint if exists postulaciones_mensaje_chk;
alter table public.postulaciones add constraint postulaciones_mensaje_chk check (mensaje is null or char_length(mensaje) <= 1000);

-- El CV solo puede apuntar a la carpeta del propio postulante.
alter table public.postulaciones drop constraint if exists postulaciones_cv_path_chk;
alter table public.postulaciones add constraint postulaciones_cv_path_chk
  check (cv_path is null or cv_path like usuario_id::text || '/%');

-- El empleador ve las postulaciones de SUS vacantes.
drop policy if exists postulaciones_select_empleador on public.postulaciones;
create policy postulaciones_select_empleador on public.postulaciones for select to authenticated
using (exists (
  select 1 from public.vacantes v
  where v.id = postulaciones.vacante_id and v.publicado_por = auth.uid()
));

-- El estado solo cambia mediante esta función (el empleador no puede editar el resto de la fila).
create or replace function public.cambiar_estado_postulacion(p_id uuid, p_estado text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_estado not in ('Pendiente','Vista','Preseleccionada','Rechazada','Contratada') then
    raise exception 'Estado no válido';
  end if;
  update public.postulaciones p
     set estado = p_estado
   where p.id = p_id
     and exists (select 1 from public.vacantes v where v.id = p.vacante_id and v.publicado_por = auth.uid());
  if not found then
    raise exception 'No tienes permiso sobre esta postulación';
  end if;
end;
$$;
revoke all on function public.cambiar_estado_postulacion(uuid, text) from public, anon;
grant execute on function public.cambiar_estado_postulacion(uuid, text) to authenticated;

-- ───────── Storage: bucket privado de CVs (máx. 5 MB, PDF/Word) ─────────
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('cvs', 'cvs', false, 5242880, array[
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document'])
on conflict (id) do update
  set public = false, file_size_limit = excluded.file_size_limit, allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists cvs_insert_propio on storage.objects;
create policy cvs_insert_propio on storage.objects for insert to authenticated
with check (bucket_id = 'cvs' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists cvs_select_propio on storage.objects;
create policy cvs_select_propio on storage.objects for select to authenticated
using (bucket_id = 'cvs' and (storage.foldername(name))[1] = auth.uid()::text);

drop policy if exists cvs_delete_propio on storage.objects;
create policy cvs_delete_propio on storage.objects for delete to authenticated
using (bucket_id = 'cvs' and (storage.foldername(name))[1] = auth.uid()::text);

-- El empleador solo lee los CVs adjuntos a postulaciones de sus vacantes.
drop policy if exists cvs_select_empleador on storage.objects;
create policy cvs_select_empleador on storage.objects for select to authenticated
using (bucket_id = 'cvs' and exists (
  select 1 from public.postulaciones p
  join public.vacantes v on v.id = p.vacante_id
  where p.cv_path = storage.objects.name and v.publicado_por = auth.uid()
));

-- Pruebas manuales (con 2 cuentas): A publica vacante con pago/documentos; B aplica con CV;
-- A ve a B y su CV; una tercera cuenta C NO debe ver el CV de B (403 al pedir la URL firmada).
