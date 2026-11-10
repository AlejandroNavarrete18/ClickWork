-- ============================================================================
--  FixNear / ClickWork  ·  Políticas de seguridad (Row Level Security)
--
--  PLANTILLA INICIAL. Antes de usarla:
--   1) Pruébala primero en un proyecto de Supabase de PRUEBA, no en el real.
--   2) Verifica que los nombres de tablas/columnas coincidan con los tuyos
--      (se dedujeron del código de la app).
--   3) Después de aplicarla, prueba: registro, login, buscar trabajadores,
--      crear/aceptar solicitud y postularse a una vacante.
--
--  Sin RLS, la clave pública que va dentro del APK permite leer/escribir
--  tablas completas. RLS es lo que realmente protege los datos.
-- ============================================================================

-- ───────────── perfiles ─────────────
alter table public.perfiles enable row level security;

drop policy if exists perfiles_select_propio        on public.perfiles;
drop policy if exists perfiles_select_trabajadores  on public.perfiles;
drop policy if exists perfiles_insert_propio        on public.perfiles;
drop policy if exists perfiles_update_propio        on public.perfiles;

-- Cada usuario ve su propio perfil
create policy perfiles_select_propio on public.perfiles
  for select to authenticated
  using (id = auth.uid());

-- Los trabajadores disponibles son visibles para quien busca servicios
-- (OJO: esto expone también su teléfono/correo. Mejora futura: una vista
--  "perfiles_publicos" sin esas columnas, y mostrar el teléfono solo cuando
--  la solicitud ya fue aceptada.)
create policy perfiles_select_trabajadores on public.perfiles
  for select to authenticated
  using (tipo_usuario = 'Trabajador' and disponible = true);

create policy perfiles_insert_propio on public.perfiles
  for insert to authenticated
  with check (id = auth.uid());

create policy perfiles_update_propio on public.perfiles
  for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

-- ───────────── solicitudes ─────────────
alter table public.solicitudes enable row level security;

drop policy if exists solicitudes_select_partes   on public.solicitudes;
drop policy if exists solicitudes_insert_cliente  on public.solicitudes;
drop policy if exists solicitudes_update_trabajador on public.solicitudes;

-- Solo el cliente y el trabajador involucrados ven la solicitud
create policy solicitudes_select_partes on public.solicitudes
  for select to authenticated
  using (cliente_id = auth.uid() or trabajador_id = auth.uid());

-- Solo puedes crear solicitudes a tu nombre
create policy solicitudes_insert_cliente on public.solicitudes
  for insert to authenticated
  with check (cliente_id = auth.uid());

-- Solo el trabajador asignado puede aceptarla
create policy solicitudes_update_trabajador on public.solicitudes
  for update to authenticated
  using (trabajador_id = auth.uid())
  with check (trabajador_id = auth.uid());

-- ───────────── postulaciones ─────────────
alter table public.postulaciones enable row level security;

drop policy if exists postulaciones_select_propia on public.postulaciones;
drop policy if exists postulaciones_insert_propia on public.postulaciones;

create policy postulaciones_select_propia on public.postulaciones
  for select to authenticated
  using (usuario_id = auth.uid());

create policy postulaciones_insert_propia on public.postulaciones
  for insert to authenticated
  with check (usuario_id = auth.uid());

-- ───────────── vacantes ─────────────
alter table public.vacantes enable row level security;

drop policy if exists vacantes_select_disponibles on public.vacantes;

-- Cualquier usuario con sesión ve las vacantes disponibles.
-- No hay políticas de insert/update: por ahora las vacantes las administras
-- tú desde el panel de Supabase (cuando exista el rol "empleador" se agregan).
create policy vacantes_select_disponibles on public.vacantes
  for select to authenticated
  using (disponible = true);
