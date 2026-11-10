-- ============================================================================
-- ClickWork · ZIP A · Marketplace de servicios (base segura)
--
-- ANTES DE EJECUTAR
--   1. Pruébala en un proyecto/rama de DESARROLLO y respalda la BD.
--   2. Revisa qué estados existen hoy:  select distinct estado from public.solicitudes;
--      Esta migración convierte 'Aceptada' en 'Confirmada'. Si aparece otro valor
--      que no esté en la lista de la sección 3, la migración fallará completa
--      (todo está en una transacción) y no cambiará nada.
--   3. Ejecútala UNA sola vez completa. Es segura de repetir salvo la
--      conversión inicial de trabajadores, que está protegida (sección 1).
--
-- QUÉ HACE
--   1. Reemplaza "tipo de usuario" por una capacidad: perfiles.ofrece_servicios.
--   2. Cierra la fuga de teléfono/correo de trabajadores (vista perfiles_publicos).
--   3. Convierte el estado de las solicitudes en una máquina de estados que solo
--      cambia mediante funciones del servidor (nadie puede saltarse pasos).
--   4. Agrega cotización, fecha programada, reprogramación y cancelación.
--
-- LÍMITES CONOCIDOS (se resuelven en ZIP D)
--   - "En curso" y "Completada" aún no usan el código de encuentro. Por ahora
--     el trabajador inicia y el CLIENTE confirma la finalización.
-- ============================================================================

begin;

-- ───────────── 1. Capacidad "ofrece servicios" ─────────────
-- Cualquier usuario busca empleo y solicita servicios. Ofrecer servicios es opcional.
do $$
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema = 'public' and table_name = 'perfiles'
      and column_name = 'ofrece_servicios'
  ) then
    alter table public.perfiles
      add column ofrece_servicios boolean not null default false;

    -- Los trabajadores que ya existían conservan su visibilidad.
    update public.perfiles set ofrece_servicios = true
    where tipo_usuario = 'Trabajador';
  end if;
end $$;

-- ───────────── 2. Perfil público sin datos de contacto ─────────────
-- La vista se ejecuta con los permisos de su dueño (comportamiento normal de las
-- vistas en Postgres). Es intencional: expone SOLO columnas seguras. El linter
-- de Supabase puede avisar "Security Definer View"; aquí es el diseño buscado.
-- Las coordenadas se redondean a 2 decimales (~1 km): nunca el domicilio exacto.
create or replace view public.perfiles_publicos as
select
  id,
  nombre,
  oficio,
  descripcion,
  disponible,
  round(latitud::numeric, 2)::double precision  as latitud,
  round(longitud::numeric, 2)::double precision as longitud
from public.perfiles
where ofrece_servicios = true
  and disponible = true;

revoke all on public.perfiles_publicos from public, anon;
grant select on public.perfiles_publicos to authenticated;

-- Esta política dejaba leer correo y teléfono de todos los trabajadores.
drop policy if exists perfiles_select_trabajadores on public.perfiles;

-- ───────────── 3. Solicitudes: columnas y estados ─────────────
alter table public.solicitudes
  add column if not exists precio_cotizado numeric(12,2)
    check (precio_cotizado is null or precio_cotizado > 0),
  add column if not exists fecha_programada timestamptz,
  add column if not exists fecha_propuesta timestamptz,
  add column if not exists fecha_propuesta_por uuid references auth.users(id),
  add column if not exists cancelada_por uuid references auth.users(id),
  add column if not exists motivo_cancelacion text,
  add column if not exists updated_at timestamptz not null default now();

-- El flujo anterior terminaba en "Aceptada"; ahora es "Confirmada" (el cliente
-- acepta la cotización).
update public.solicitudes set estado = 'Confirmada' where estado = 'Aceptada';

alter table public.solicitudes drop constraint if exists solicitudes_estado_check;
alter table public.solicitudes add constraint solicitudes_estado_check
  check (estado in ('Pendiente','Cotizada','Confirmada','En curso',
                    'Completada','Cancelada','Rechazada'));

-- Ya no se escribe directo en la tabla: todo pasa por las funciones de abajo.
-- (Antes cualquier cliente podía insertar una solicitud ya "Completada", y el
-- trabajador podía editar cualquier columna.)
drop policy if exists solicitudes_insert_cliente   on public.solicitudes;
drop policy if exists solicitudes_update_trabajador on public.solicitudes;
-- Se conserva solicitudes_select_partes: cliente y trabajador ven su solicitud.

-- ───────────── 4. Funciones de negocio ─────────────

-- 4.1 Crear solicitud
create or replace function public.crear_solicitud(
  p_trabajador_id uuid,
  p_servicio      text,
  p_descripcion   text,
  p_lat           double precision,
  p_lng           double precision,
  p_direccion     text,
  p_fecha         timestamptz default null
) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  v_cli  public.perfiles%rowtype;
  v_trab public.perfiles%rowtype;
  v_id   uuid;
begin
  if auth.uid() is null then raise exception 'Sesión no válida'; end if;
  if p_trabajador_id = auth.uid() then
    raise exception 'No puedes solicitarte un servicio a ti mismo';
  end if;

  select * into v_cli from public.perfiles where id = auth.uid();
  if not found then raise exception 'Perfil no encontrado'; end if;

  select * into v_trab from public.perfiles
   where id = p_trabajador_id and ofrece_servicios = true and disponible = true;
  if not found then raise exception 'Este trabajador ya no está disponible'; end if;

  if char_length(coalesce(trim(p_descripcion), '')) < 5 then
    raise exception 'Describe lo que necesitas (mínimo 5 caracteres)';
  end if;
  if p_fecha is not null and p_fecha <= now() then
    raise exception 'La fecha debe ser futura';
  end if;
  if (select count(*) from public.solicitudes
       where cliente_id = auth.uid() and trabajador_id = p_trabajador_id
         and estado in ('Pendiente','Cotizada')) >= 3 then
    raise exception 'Ya tienes varias solicitudes abiertas con este trabajador';
  end if;

  -- Los teléfonos quedan vacíos: se llenan hasta que el cliente confirma.
  insert into public.solicitudes (
    cliente_id, trabajador_id, servicio, descripcion, estado,
    latitud_cliente, longitud_cliente, direccion_cliente,
    nombre_cliente, telefono_cliente, nombre_trabajador, telefono_trabajador,
    fecha_programada
  ) values (
    auth.uid(), p_trabajador_id,
    left(coalesce(nullif(trim(p_servicio), ''), 'Servicio general'), 120),
    left(trim(p_descripcion), 1000),
    'Pendiente',
    p_lat, p_lng, left(coalesce(p_direccion, ''), 300),
    v_cli.nombre, '', v_trab.nombre, '',
    p_fecha
  ) returning id into v_id;

  return v_id;
end $$;

-- 4.2 El trabajador cotiza (o corrige su precio mientras no esté confirmada)
create or replace function public.cotizar_solicitud(p_id uuid, p_precio numeric)
returns void
language plpgsql security definer set search_path = public as $$
declare s public.solicitudes%rowtype;
begin
  select * into s from public.solicitudes where id = p_id for update;
  if not found then raise exception 'Solicitud no encontrada'; end if;
  if s.trabajador_id is distinct from auth.uid() then raise exception 'Sin permiso'; end if;
  if s.estado not in ('Pendiente','Cotizada') then
    raise exception 'Ya no se puede cotizar una solicitud en estado %', s.estado;
  end if;
  if p_precio is null or p_precio <= 0 or p_precio > 1000000 then
    raise exception 'El precio debe estar entre 1 y 1,000,000 MXN';
  end if;

  update public.solicitudes
     set estado = 'Cotizada', precio_cotizado = round(p_precio, 2), updated_at = now()
   where id = p_id;
end $$;

-- 4.3 Cambios de estado permitidos
--   Pendiente|Cotizada -> Rechazada   (trabajador)
--   Cotizada           -> Confirmada  (cliente; aquí se comparten los teléfonos)
--   Pendiente|Cotizada|Confirmada -> Cancelada (cualquiera de las dos partes)
--   Confirmada         -> En curso    (trabajador)         [provisional]
--   En curso           -> Completada  (cliente)            [provisional]
create or replace function public.cambiar_estado_solicitud(
  p_id uuid, p_nuevo text, p_motivo text default null
) returns void
language plpgsql security definer set search_path = public as $$
declare
  s          public.solicitudes%rowtype;
  es_cliente boolean;
  es_trab    boolean;
begin
  select * into s from public.solicitudes where id = p_id for update;
  if not found then raise exception 'Solicitud no encontrada'; end if;

  es_cliente := s.cliente_id    = auth.uid();
  es_trab    := s.trabajador_id = auth.uid();
  if not (es_cliente or es_trab) then raise exception 'Sin permiso'; end if;

  if not (
    (s.estado in ('Pendiente','Cotizada') and p_nuevo = 'Rechazada'  and es_trab) or
    (s.estado = 'Cotizada'                and p_nuevo = 'Confirmada' and es_cliente) or
    (s.estado in ('Pendiente','Cotizada','Confirmada') and p_nuevo = 'Cancelada') or
    (s.estado = 'Confirmada'              and p_nuevo = 'En curso'   and es_trab) or
    (s.estado = 'En curso'                and p_nuevo = 'Completada' and es_cliente)
  ) then
    raise exception 'Cambio no permitido: % -> %', s.estado, p_nuevo;
  end if;

  update public.solicitudes set
    estado = p_nuevo,
    updated_at = now(),
    cancelada_por = case when p_nuevo = 'Cancelada' then auth.uid() else cancelada_por end,
    motivo_cancelacion = case when p_nuevo = 'Cancelada'
                              then left(nullif(trim(p_motivo), ''), 300)
                              else motivo_cancelacion end,
    telefono_cliente = case when p_nuevo = 'Confirmada'
      then coalesce((select telefono from public.perfiles where id = s.cliente_id), '')
      else telefono_cliente end,
    telefono_trabajador = case when p_nuevo = 'Confirmada'
      then coalesce((select telefono from public.perfiles where id = s.trabajador_id), '')
      else telefono_trabajador end
  where id = p_id;
end $$;

-- 4.4 Reprogramar: una parte propone, la otra acepta o rechaza
create or replace function public.proponer_fecha(p_id uuid, p_fecha timestamptz)
returns void
language plpgsql security definer set search_path = public as $$
declare s public.solicitudes%rowtype;
begin
  select * into s from public.solicitudes where id = p_id for update;
  if not found then raise exception 'Solicitud no encontrada'; end if;
  if auth.uid() not in (s.cliente_id, s.trabajador_id) then
    raise exception 'Sin permiso';
  end if;
  if s.estado not in ('Pendiente','Cotizada','Confirmada') then
    raise exception 'Ya no se puede reprogramar una solicitud en estado %', s.estado;
  end if;
  if p_fecha is null or p_fecha <= now() then
    raise exception 'La fecha debe ser futura';
  end if;

  update public.solicitudes
     set fecha_propuesta = p_fecha, fecha_propuesta_por = auth.uid(), updated_at = now()
   where id = p_id;
end $$;

create or replace function public.responder_fecha(p_id uuid, p_acepta boolean)
returns void
language plpgsql security definer set search_path = public as $$
declare s public.solicitudes%rowtype;
begin
  select * into s from public.solicitudes where id = p_id for update;
  if not found then raise exception 'Solicitud no encontrada'; end if;
  if auth.uid() not in (s.cliente_id, s.trabajador_id) then
    raise exception 'Sin permiso';
  end if;
  if s.fecha_propuesta is null then raise exception 'No hay una fecha propuesta'; end if;
  if s.fecha_propuesta_por = auth.uid() then
    raise exception 'La otra persona debe responder tu propuesta';
  end if;
  if s.estado not in ('Pendiente','Cotizada','Confirmada') then
    raise exception 'La solicitud ya no admite cambios de fecha';
  end if;

  update public.solicitudes set
    fecha_programada = case when p_acepta then fecha_propuesta else fecha_programada end,
    fecha_propuesta = null,
    fecha_propuesta_por = null,
    updated_at = now()
  where id = p_id;
end $$;

-- 4.5 Permisos: solo usuarios autenticados pueden ejecutarlas
revoke all on function public.crear_solicitud(uuid, text, text, double precision, double precision, text, timestamptz) from public, anon;
revoke all on function public.cotizar_solicitud(uuid, numeric)            from public, anon;
revoke all on function public.cambiar_estado_solicitud(uuid, text, text)  from public, anon;
revoke all on function public.proponer_fecha(uuid, timestamptz)           from public, anon;
revoke all on function public.responder_fecha(uuid, boolean)              from public, anon;

grant execute on function public.crear_solicitud(uuid, text, text, double precision, double precision, text, timestamptz) to authenticated;
grant execute on function public.cotizar_solicitud(uuid, numeric)            to authenticated;
grant execute on function public.cambiar_estado_solicitud(uuid, text, text)  to authenticated;
grant execute on function public.proponer_fecha(uuid, timestamptz)           to authenticated;
grant execute on function public.responder_fecha(uuid, boolean)              to authenticated;

commit;

-- ============================================================================
-- PRUEBAS MANUALES (con 3 cuentas: cliente A, trabajador B, ajena C)
--   1. A solicita a B  -> estado Pendiente, teléfonos vacíos.
--   2. C no ve esa solicitud:  select * from solicitudes;  (0 filas para C)
--   3. B cotiza -> Cotizada.  A confirma -> Confirmada y ya se ven teléfonos.
--   4. A intenta pasar directo a 'Completada' -> "Cambio no permitido".
--   5. B inicia -> En curso. A completa -> Completada.
--   6. Reprogramar: B propone fecha; B no puede aceptar su propia propuesta; A sí.
--   7. A (autenticado) consulta perfiles?select=correo  -> no devuelve datos de B.
--      perfiles_publicos?select=*  -> sin correo ni teléfono, coordenadas redondeadas.
-- ============================================================================
