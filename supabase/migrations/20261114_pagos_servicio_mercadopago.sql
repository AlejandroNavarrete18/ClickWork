-- ClickWork · Pagos de servicios (cliente -> trabajador) con Mercado Pago
-- Ejecutar DESPUÉS de 20261113_zip_b_marketplace.sql. Probar primero en desarrollo.
--
-- Reglas de seguridad de este diseño:
--   * La app NUNCA escribe en pagos_servicio (ni insert ni update). Solo lee los suyos.
--   * El monto sale de solicitudes.precio_cotizado en el servidor, no del celular.
--   * Solo la Edge Function mercadopago-webhook (service_role) marca un pago 'aprobado',
--     después de consultar el pago directamente a la API de Mercado Pago.
--   * Una solicitud solo puede tener UN pago aprobado (índice único parcial).

begin;

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

commit;

-- Pruebas mínimas (con 3 cuentas: cliente, trabajador, ajena):
--   1. Cliente:    insert into pagos_servicio ...        -> debe fallar (permission denied)
--   2. Cliente y trabajador: select * from pagos_servicio -> ven solo sus pagos
--   3. Cuenta ajena: select * from pagos_servicio         -> 0 filas
--   4. Cancelar una solicitud con pago aprobado           -> error con mensaje de soporte
