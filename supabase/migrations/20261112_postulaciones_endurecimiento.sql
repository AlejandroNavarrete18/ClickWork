-- ClickWork — Endurecimiento de postulaciones (corrige huecos detectados al revisar 20261111_vacantes_pro.sql)
-- Es aditiva y NO borra datos. Ejecútala DESPUÉS de 20261111_vacantes_pro.sql, primero en una rama de desarrollo y con respaldo.
--
-- Antes de ejecutar, revisa (solo lectura):
--   1) ¿Hay un CHECK sobre postulaciones.estado que bloquee 'Preseleccionada' / 'Contratada' / 'Rechazada' / 'Vista'?
--      select conname, pg_get_constraintdef(oid) from pg_constraint
--      where conrelid = 'public.postulaciones'::regclass and contype = 'c';
--   2) ¿Los tipos coinciden (ambos uuid)?
--      select table_name, column_name, data_type from information_schema.columns
--      where table_schema = 'public' and (table_name, column_name) in (('postulaciones','vacante_id'),('vacantes','id'),('postulaciones','usuario_id'));
--   3) ¿Hay postulaciones duplicadas? (el bloque 2 solo crea el índice si no las hay)
--      select vacante_id, usuario_id, count(*) from public.postulaciones group by 1,2 having count(*) > 1;

-- ───────── 1) El postulante NO puede crearse una postulación ya "Contratada" ─────────
-- La política anterior solo exigía usuario_id = auth.uid(): con la API REST se podía insertar
-- estado = 'Contratada'. Ahora solo se acepta 'Pendiente' y únicamente sobre vacantes disponibles.
-- (Si vuelves a ejecutar supabase/politicas_rls.sql esta política se reemplaza por la anterior: vuelve a correr este archivo.)
drop policy if exists postulaciones_insert_propia on public.postulaciones;
create policy postulaciones_insert_propia on public.postulaciones
  for insert to authenticated
  with check (
    usuario_id = auth.uid()
    and coalesce(estado, 'Pendiente') = 'Pendiente'
    and exists (
      select 1 from public.vacantes v
      where v.id = postulaciones.vacante_id and v.disponible = true
    )
  );

-- ───────── 2) Unicidad (vacante_id, usuario_id) ─────────
-- La app inserta con on_conflict=vacante_id,usuario_id: PostgREST exige un índice único con esas columnas,
-- y ese índice no estaba definido en ninguna migración del repositorio.
do $$
begin
  if exists (
    select 1 from public.postulaciones group by vacante_id, usuario_id having count(*) > 1
  ) then
    raise notice 'Hay postulaciones duplicadas por (vacante_id, usuario_id). Límpialas y vuelve a ejecutar este bloque.';
  else
    create unique index if not exists postulaciones_vacante_usuario_uq
      on public.postulaciones (vacante_id, usuario_id);
  end if;
end $$;

-- ───────── 3) (Opcional) Si el paso 1 de arriba mostró un CHECK sobre estado ─────────
-- Sustituye el nombre real de la restricción y ejecuta:
--   alter table public.postulaciones drop constraint <nombre_del_check>;
--   alter table public.postulaciones add constraint postulaciones_estado_chk
--     check (estado in ('Pendiente','Vista','Preseleccionada','Rechazada','Contratada'));
