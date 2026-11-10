-- FixNear V3 - migración aditiva para publicación de vacantes y empresas.
-- Ejecutar primero en un proyecto Supabase de desarrollo y respaldar la BD.
-- No elimina tablas ni columnas existentes.

create extension if not exists pgcrypto;

-- Empresas y miembros (una cuenta puede pertenecer a varias empresas).
create table if not exists public.empresas (
  id uuid primary key default gen_random_uuid(),
  nombre text not null,
  descripcion text,
  correo_contacto text,
  telefono_contacto text,
  sitio_web text,
  direccion text,
  verificada boolean not null default false,
  estado text not null default 'pendiente' check (estado in ('pendiente','activa','rechazada','suspendida')),
  creado_por uuid not null references auth.users(id) on delete restrict,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.empresa_miembros (
  empresa_id uuid not null references public.empresas(id) on delete cascade,
  usuario_id uuid not null references auth.users(id) on delete cascade,
  rol text not null default 'reclutador' check (rol in ('propietario','admin_empresa','reclutador')),
  estado text not null default 'activo' check (estado in ('invitado','activo','suspendido')),
  created_at timestamptz not null default now(),
  primary key (empresa_id, usuario_id)
);

-- Extiende la tabla existente que utiliza EmpleosScreen y SupabaseApi.
alter table public.vacantes
  add column if not exists publicado_por uuid references auth.users(id) on delete set null,
  add column if not exists empresa_id uuid references public.empresas(id) on delete set null,
  add column if not exists tipo_jornada text,
  add column if not exists modalidad text,
  add column if not exists sueldo_minimo numeric(12,2),
  add column if not exists sueldo_maximo numeric(12,2),
  add column if not exists fecha_cierre timestamptz,
  add column if not exists actualizado_en timestamptz not null default now();

create index if not exists idx_vacantes_publicado_por on public.vacantes(publicado_por);
create index if not exists idx_vacantes_empresa_id on public.vacantes(empresa_id);

-- Suscripciones: el estado real debe sincronizarse desde el backend/webhook.
create table if not exists public.planes (
  id uuid primary key default gen_random_uuid(),
  codigo text not null unique,
  nombre text not null,
  precio_mxn numeric(12,2) not null default 0 check (precio_mxn >= 0),
  vacantes_activas_max integer,
  reclutadores_max integer,
  funciones_ia boolean not null default false,
  activo boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.suscripciones (
  id uuid primary key default gen_random_uuid(),
  empresa_id uuid not null references public.empresas(id) on delete restrict,
  plan_id uuid not null references public.planes(id) on delete restrict,
  proveedor text not null default 'mercadopago',
  proveedor_suscripcion_id text unique,
  estado text not null default 'pendiente' check (estado in ('pendiente','autorizada','pausada','cancelada','vencida','en_revision')),
  periodo_inicio timestamptz,
  periodo_fin timestamptz,
  cancelada_en timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.pagos_suscripcion (
  id uuid primary key default gen_random_uuid(),
  suscripcion_id uuid not null references public.suscripciones(id) on delete restrict,
  proveedor text not null,
  proveedor_pago_id text unique,
  monto_mxn numeric(12,2) not null check (monto_mxn >= 0),
  moneda text not null default 'MXN',
  estado text not null check (estado in ('pendiente','aprobado','rechazado','reembolsado','cancelado')),
  created_at timestamptz not null default now()
);

create table if not exists public.webhook_eventos (
  id uuid primary key default gen_random_uuid(),
  proveedor text not null,
  evento_id text not null,
  tipo text,
  procesado boolean not null default false,
  recibido_en timestamptz not null default now(),
  unique(proveedor, evento_id)
);

create table if not exists public.consumo_ia (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references auth.users(id) on delete cascade,
  empresa_id uuid references public.empresas(id) on delete set null,
  funcion text not null,
  proveedor text,
  tokens integer not null default 0,
  costo_estimado_mxn numeric(12,4) not null default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.reportes_contenido (
  id uuid primary key default gen_random_uuid(),
  reportado_por uuid not null references auth.users(id) on delete restrict,
  tipo_objetivo text not null check (tipo_objetivo in ('usuario','empresa','vacante','servicio')),
  objetivo_id uuid not null,
  motivo text not null,
  estado text not null default 'pendiente' check (estado in ('pendiente','en_revision','resuelto','descartado')),
  created_at timestamptz not null default now()
);

-- RLS: las tablas administrativas y financieras no aceptan escrituras desde clientes.
alter table public.empresas enable row level security;
alter table public.empresa_miembros enable row level security;
alter table public.planes enable row level security;
alter table public.suscripciones enable row level security;
alter table public.pagos_suscripcion enable row level security;
alter table public.webhook_eventos enable row level security;
alter table public.consumo_ia enable row level security;
alter table public.reportes_contenido enable row level security;

-- Empresas: miembros activos pueden consultar su organización; creador puede crear.
drop policy if exists empresas_select_miembros on public.empresas;
create policy empresas_select_miembros on public.empresas for select to authenticated
using (creado_por = auth.uid() or exists (
  select 1 from public.empresa_miembros m
  where m.empresa_id = empresas.id and m.usuario_id = auth.uid() and m.estado = 'activo'
));
drop policy if exists empresas_insert_creador on public.empresas;
create policy empresas_insert_creador on public.empresas for insert to authenticated
with check (creado_por = auth.uid());

-- Miembros solo ven su propia membresía. Invitaciones y roles deben gestionarse desde backend.
drop policy if exists empresa_miembros_select_propio on public.empresa_miembros;
create policy empresa_miembros_select_propio on public.empresa_miembros for select to authenticated
using (usuario_id = auth.uid());

-- Los planes activos son públicos para que la app pueda mostrarlos.
drop policy if exists planes_select_activos on public.planes;
create policy planes_select_activos on public.planes for select to authenticated using (activo = true);

-- Suscripciones: solo lectura para miembros de la empresa. No permitir INSERT/UPDATE desde APK.
drop policy if exists suscripciones_select_miembros on public.suscripciones;
create policy suscripciones_select_miembros on public.suscripciones for select to authenticated
using (exists (
  select 1 from public.empresa_miembros m
  where m.empresa_id = suscripciones.empresa_id and m.usuario_id = auth.uid() and m.estado = 'activo'
));

-- Pagos, webhooks y auditoría de IA: no se exponen a usuarios por defecto.
-- Las Edge Functions con service role deben validar autorización y registrar auditoría.

-- Publicación individual: solo se puede insertar vacante a nombre del usuario autenticado.
drop policy if exists vacantes_insert_propias on public.vacantes;
create policy vacantes_insert_propias on public.vacantes for insert to authenticated
with check (publicado_por = auth.uid() and disponible = true);

drop policy if exists vacantes_update_propias on public.vacantes;
create policy vacantes_update_propias on public.vacantes for update to authenticated
using (publicado_por = auth.uid())
with check (publicado_por = auth.uid());

-- Los registros antiguos sin publicado_por conservan su lectura pública según la política existente.
