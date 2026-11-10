-- ClickWork · Pagos de servicios (todo en uno). Ejecutar UNA vez en Supabase → SQL Editor.
-- Requiere haber ejecutado antes la migración 20261110 (solicitudes) y 20261113 (notificaciones).

begin;

-- ClickWork · Pagos de servicios (cliente -> trabajador) con Mercado Pago
-- Ejecutar DESPUÉS de 20261113_zip_b_marketplace.sql. Probar primero en desarrollo.
--
-- Reglas de seguridad de este diseño:
--   * La app NUNCA escribe en pagos_servicio (ni insert ni update). Solo lee los suyos.
--   * El monto sale de solicitudes.precio_cotizado en el servidor, no del celular.
--   * Solo la Edge Function mercadopago-webhook (service_role) marca un pago 'aprobado',
--     después de consultar el pago directamente a la API de Mercado Pago.
--   * Una solicitud solo puede tener UN pago aprobado (índice único parcial).


create table if not exists public.pagos_servicio (
  id uuid primary key default gen_random_uuid(),
  solicitud_id uuid not null references public.solicitudes(id) on delete restrict,
  cliente_id uuid not null references auth.users(id) on delete restrict,
  trabajador_id uuid not null references auth.users(id) on delete restrict,
  monto_mxn numeric(12,2) not null check (monto_mxn > 0),
  moneda text not null default 'MXN' check (moneda = 'MXN'),
  proveedor text not null default 'mercadopago',
  estado text not null default 'pendiente'
    check (estado in ('pendiente','aprobado','rechazado','cancelado','reembolsado','en_revision')),
  preferencia_id text,
  proveedor_pago_id text unique,
  pagado_en timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists pagos_servicio_solicitud_idx on public.pagos_servicio(solicitud_id, created_at desc);
create index if not exists pagos_servicio_cliente_idx on public.pagos_servicio(cliente_id);
create index if not exists pagos_servicio_trabajador_idx on public.pagos_servicio(trabajador_id);

-- Evita cobrar dos veces el mismo servicio.
create unique index if not exists pagos_servicio_un_aprobado_por_solicitud
  on public.pagos_servicio(solicitud_id) where estado = 'aprobado';

alter table public.pagos_servicio enable row level security;

-- Solo lectura para las dos partes. Sin políticas de escritura: el APK no puede crear ni cambiar pagos.
drop policy if exists pagos_servicio_select_partes on public.pagos_servicio;
create policy pagos_servicio_select_partes on public.pagos_servicio
  for select to authenticated
  using (auth.uid() in (cliente_id, trabajador_id));

revoke insert, update, delete on public.pagos_servicio from anon, authenticated;
revoke all on public.pagos_servicio from anon;
grant select on public.pagos_servicio to authenticated;

-- Una solicitud con pago aprobado no se puede cancelar desde la app:
-- hay dinero de por medio y el reembolso debe decidirse (y ejecutarse) a mano o por soporte.
-- (Es la misma función de 20261110 con una sola verificación nueva.)
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

  if p_nuevo = 'Cancelada' and exists (
    select 1 from public.pagos_servicio
    where solicitud_id = p_id and estado in ('aprobado','en_revision')
  ) then
    raise exception 'Esta solicitud ya tiene un pago. Para cancelarla contacta a soporte y se gestiona el reembolso.';
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

revoke all on function public.cambiar_estado_solicitud(uuid, text, text) from public, anon;
grant execute on function public.cambiar_estado_solicitud(uuid, text, text) to authenticated;

-- ClickWork · Más métodos de pago de servicios
-- Ejecutar DESPUÉS de 20261114_pagos_servicio_mercadopago.sql.
--
-- Métodos:
--   * En línea (tarjeta crédito/débito, SPEI, OXXO, saldo): los procesa Mercado Pago;
--     solo el webhook puede dejarlos en 'aprobado'.
--   * Efectivo y transferencia directa: la app NO mueve dinero, solo deja constancia.
--       - Cliente avisa "ya pagué"   -> 'por_confirmar' (el trabajador debe confirmar)
--       - Trabajador registra pago   -> 'confirmado'   (el trabajador atestigua que lo recibió)
--     Todo pasa por funciones del servidor: el APK no escribe en pagos_servicio.


alter table public.pagos_servicio
  add column if not exists metodo text not null default 'mercadopago'
    check (metodo in ('mercadopago','efectivo','transferencia')),
  add column if not exists registrado_por uuid references auth.users(id);

alter table public.pagos_servicio drop constraint if exists pagos_servicio_estado_check;
alter table public.pagos_servicio add constraint pagos_servicio_estado_check
  check (estado in ('pendiente','aprobado','rechazado','cancelado','reembolsado','en_revision','por_confirmar','confirmado'));

-- Un solo pago "cobrado" por solicitud (en línea aprobado o manual confirmado).
drop index if exists public.pagos_servicio_un_aprobado_por_solicitud;
create unique index if not exists pagos_servicio_un_cobrado_por_solicitud
  on public.pagos_servicio(solicitud_id) where estado in ('aprobado','confirmado');

-- ───────── Registrar pago en efectivo / transferencia ─────────
create or replace function public.registrar_pago_manual(p_solicitud_id uuid, p_metodo text)
returns uuid
language plpgsql security definer set search_path = public as $$
declare
  s public.solicitudes%rowtype;
  v_id uuid;
  v_es_trab boolean;
  v_estado text;
begin
  if auth.uid() is null then raise exception 'Sesión no válida'; end if;
  if p_metodo not in ('efectivo','transferencia') then raise exception 'Método no válido'; end if;

  select * into s from public.solicitudes where id = p_solicitud_id for update;
  if not found then raise exception 'Solicitud no encontrada'; end if;
  if auth.uid() not in (s.cliente_id, s.trabajador_id) then raise exception 'Sin permiso'; end if;
  if s.estado not in ('Confirmada','En curso','Completada') then
    raise exception 'La solicitud aún no está lista para registrar un pago';
  end if;
  if s.precio_cotizado is null or s.precio_cotizado <= 0 then
    raise exception 'La solicitud no tiene precio';
  end if;
  if exists (select 1 from public.pagos_servicio
             where solicitud_id = p_solicitud_id and estado in ('aprobado','confirmado','por_confirmar','en_revision')) then
    raise exception 'Esta solicitud ya tiene un pago registrado';
  end if;

  v_es_trab := (auth.uid() = s.trabajador_id);
  v_estado  := case when v_es_trab then 'confirmado' else 'por_confirmar' end;

  insert into public.pagos_servicio
    (solicitud_id, cliente_id, trabajador_id, monto_mxn, proveedor, metodo, estado, registrado_por, pagado_en)
  values
    (s.id, s.cliente_id, s.trabajador_id, s.precio_cotizado, 'manual', p_metodo, v_estado, auth.uid(),
     case when v_es_trab then now() else null end)
  returning id into v_id;

  insert into public.notificaciones(usuario_id, tipo, titulo, cuerpo, referencia_id)
  values (
    case when v_es_trab then s.cliente_id else s.trabajador_id end,
    'solicitud',
    case when v_es_trab then 'El trabajador registró tu pago' else 'El cliente indica que ya pagó' end,
    case when v_es_trab then 'Pago en ' || p_metodo || ' confirmado.'
         else 'Pago en ' || p_metodo || '. Confírmalo en Mis solicitudes si ya lo recibiste.' end,
    s.id
  );
  return v_id;
end $$;

-- ───────── El trabajador confirma (o niega) un pago manual avisado por el cliente ─────────
create or replace function public.responder_pago_manual(p_pago_id uuid, p_recibido boolean)
returns void
language plpgsql security definer set search_path = public as $$
declare p public.pagos_servicio%rowtype;
begin
  if auth.uid() is null then raise exception 'Sesión no válida'; end if;
  select * into p from public.pagos_servicio where id = p_pago_id for update;
  if not found then raise exception 'Pago no encontrado'; end if;
  if p.trabajador_id <> auth.uid() then raise exception 'Solo el trabajador puede confirmar este pago'; end if;
  if p.proveedor <> 'manual' or p.estado <> 'por_confirmar' then
    raise exception 'Este pago ya no está pendiente de confirmación';
  end if;

  if p_recibido and exists (select 1 from public.pagos_servicio
        where solicitud_id = p.solicitud_id and id <> p.id and estado in ('aprobado','confirmado')) then
    raise exception 'Esta solicitud ya tiene un pago registrado';
  end if;

  update public.pagos_servicio set
    estado = case when p_recibido then 'confirmado' else 'rechazado' end,
    pagado_en = case when p_recibido then now() else null end,
    updated_at = now()
  where id = p.id;

  insert into public.notificaciones(usuario_id, tipo, titulo, cuerpo, referencia_id)
  values (p.cliente_id, 'solicitud',
    case when p_recibido then 'Tu pago fue confirmado' else 'El trabajador no recibió el pago' end,
    case when p_recibido then 'El trabajador confirmó que recibió tu pago.'
         else 'Aclara el pago con el trabajador por el chat.' end,
    p.solicitud_id);
end $$;

-- ───────── El cliente retira su aviso de pago (se equivocó) ─────────
create or replace function public.cancelar_pago_manual(p_pago_id uuid)
returns void
language plpgsql security definer set search_path = public as $$
declare p public.pagos_servicio%rowtype;
begin
  if auth.uid() is null then raise exception 'Sesión no válida'; end if;
  select * into p from public.pagos_servicio where id = p_pago_id for update;
  if not found then raise exception 'Pago no encontrado'; end if;
  if p.cliente_id <> auth.uid() or p.registrado_por is distinct from auth.uid() then
    raise exception 'Sin permiso';
  end if;
  if p.proveedor <> 'manual' or p.estado <> 'por_confirmar' then
    raise exception 'Este aviso ya no se puede retirar';
  end if;
  update public.pagos_servicio set estado = 'cancelado', updated_at = now() where id = p.id;
end $$;

revoke all on function public.registrar_pago_manual(uuid, text) from public, anon;
revoke all on function public.responder_pago_manual(uuid, boolean) from public, anon;
revoke all on function public.cancelar_pago_manual(uuid) from public, anon;
grant execute on function public.registrar_pago_manual(uuid, text) to authenticated;
grant execute on function public.responder_pago_manual(uuid, boolean) to authenticated;
grant execute on function public.cancelar_pago_manual(uuid) to authenticated;

-- ───────── No cancelar solicitudes con pago (ahora incluye pagos manuales) ─────────
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

  if p_nuevo = 'Cancelada' and exists (
    select 1 from public.pagos_servicio
    where solicitud_id = p_id and estado in ('aprobado','confirmado','por_confirmar','en_revision')
  ) then
    raise exception 'Esta solicitud ya tiene un pago. Para cancelarla contacta a soporte y se gestiona el reembolso.';
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

revoke all on function public.cambiar_estado_solicitud(uuid, text, text) from public, anon;
grant execute on function public.cambiar_estado_solicitud(uuid, text, text) to authenticated;

commit;
