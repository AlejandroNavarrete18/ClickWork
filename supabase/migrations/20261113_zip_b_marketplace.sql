-- ClickWork ZIP B · Marketplace de servicios, chat, reseñas y notificaciones
-- Ejecutar DESPUÉS de 20261110_zip_a_marketplace.sql y 20261112_postulaciones_endurecimiento.sql.
-- Probar primero en una rama/proyecto de desarrollo y respaldar la base.
-- No elimina datos ni modifica las tablas heredadas.

begin;

create extension if not exists pgcrypto;

-- Categorías administrables del marketplace.
create table if not exists public.categorias_servicio (
  id uuid primary key default gen_random_uuid(),
  nombre text not null unique,
  icono text not null default 'work',
  color_hex text not null default '#6D5BFF',
  activa boolean not null default true,
  orden integer not null default 100,
  created_at timestamptz not null default now()
);

insert into public.categorias_servicio(nombre, icono, color_hex, orden) values
 ('Hogar y mantenimiento','home','#F59E0B',10),
 ('Limpieza','cleaning_services','#14B8A6',20),
 ('Belleza y bienestar','spa','#EC4899',30),
 ('Tecnología','devices','#38BDF8',40),
 ('Construcción','construction','#F97316',50),
 ('Transporte y mudanzas','local_shipping','#8B5CF6',60),
 ('Clases y asesorías','school','#22C55E',70),
 ('Eventos','celebration','#E879F9',80),
 ('Otros servicios','more_horiz','#64748B',90)
on conflict (nombre) do nothing;

-- Catálogo de servicios ofrecidos por cada trabajador.
create table if not exists public.servicios_trabajador (
  id uuid primary key default gen_random_uuid(),
  trabajador_id uuid not null references auth.users(id) on delete cascade,
  categoria_id uuid references public.categorias_servicio(id) on delete set null,
  nombre text not null check (char_length(trim(nombre)) between 3 and 100),
  descripcion text not null default '' check (char_length(descripcion) <= 1200),
  precio numeric(12,2) check (precio is null or precio >= 0),
  tipo_precio text not null default 'Por servicio'
    check (tipo_precio in ('Por servicio','Por hora','Por día','Desde','A cotizar')),
  activo boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists servicios_trabajador_activos_idx
  on public.servicios_trabajador(trabajador_id, activo);

-- Disponibilidad semanal: día 0 = domingo ... 6 = sábado.
create table if not exists public.disponibilidad_trabajador (
  id uuid primary key default gen_random_uuid(),
  trabajador_id uuid not null references auth.users(id) on delete cascade,
  dia_semana smallint not null check (dia_semana between 0 and 6),
  hora_inicio time not null,
  hora_fin time not null,
  activo boolean not null default true,
  check (hora_fin > hora_inicio),
  unique (trabajador_id, dia_semana, hora_inicio, hora_fin)
);

-- Una conversación por solicitud; los participantes se derivan de solicitudes.
create table if not exists public.conversaciones (
  id uuid primary key default gen_random_uuid(),
  solicitud_id uuid not null unique references public.solicitudes(id) on delete cascade,
  creada_en timestamptz not null default now(),
  actualizada_en timestamptz not null default now()
);

create table if not exists public.mensajes (
  id uuid primary key default gen_random_uuid(),
  conversacion_id uuid not null references public.conversaciones(id) on delete cascade,
  remitente_id uuid not null references auth.users(id) on delete cascade,
  contenido text not null check (char_length(trim(contenido)) between 1 and 3000),
  leido_en timestamptz,
  creado_en timestamptz not null default now()
);
create index if not exists mensajes_conversacion_fecha_idx
  on public.mensajes(conversacion_id, creado_en);

-- Reseña de una parte a la otra, solo después de completar el servicio.
create table if not exists public.resenas (
  id uuid primary key default gen_random_uuid(),
  solicitud_id uuid not null references public.solicitudes(id) on delete cascade,
  autor_id uuid not null references auth.users(id) on delete cascade,
  destinatario_id uuid not null references auth.users(id) on delete cascade,
  puntuacion smallint not null check (puntuacion between 1 and 5),
  comentario text not null default '' check (char_length(comentario) <= 1000),
  creada_en timestamptz not null default now(),
  unique (solicitud_id, autor_id),
  check (autor_id <> destinatario_id)
);
create index if not exists resenas_destinatario_idx on public.resenas(destinatario_id, creada_en desc);

-- Centro de notificaciones dentro de la app.
create table if not exists public.notificaciones (
  id uuid primary key default gen_random_uuid(),
  usuario_id uuid not null references auth.users(id) on delete cascade,
  tipo text not null check (tipo in ('solicitud','cotizacion','mensaje','resena','vacante','sistema')),
  titulo text not null check (char_length(titulo) between 1 and 120),
  cuerpo text not null default '' check (char_length(cuerpo) <= 500),
  referencia_id uuid,
  leida boolean not null default false,
  creada_en timestamptz not null default now()
);
create index if not exists notificaciones_usuario_fecha_idx
  on public.notificaciones(usuario_id, creada_en desc);

-- Seguridad por fila.
alter table public.categorias_servicio enable row level security;
alter table public.servicios_trabajador enable row level security;
alter table public.disponibilidad_trabajador enable row level security;
alter table public.conversaciones enable row level security;
alter table public.mensajes enable row level security;
alter table public.resenas enable row level security;
alter table public.notificaciones enable row level security;

drop policy if exists categorias_servicio_lectura on public.categorias_servicio;
create policy categorias_servicio_lectura on public.categorias_servicio
 for select to authenticated using (activa = true);

drop policy if exists servicios_lectura on public.servicios_trabajador;
create policy servicios_lectura on public.servicios_trabajador
 for select to authenticated using (activo = true or trabajador_id = auth.uid());
drop policy if exists servicios_propios_insert on public.servicios_trabajador;
create policy servicios_propios_insert on public.servicios_trabajador
 for insert to authenticated with check (trabajador_id = auth.uid());
drop policy if exists servicios_propios_update on public.servicios_trabajador;
create policy servicios_propios_update on public.servicios_trabajador
 for update to authenticated using (trabajador_id = auth.uid()) with check (trabajador_id = auth.uid());
drop policy if exists servicios_propios_delete on public.servicios_trabajador;
create policy servicios_propios_delete on public.servicios_trabajador
 for delete to authenticated using (trabajador_id = auth.uid());

drop policy if exists disponibilidad_lectura on public.disponibilidad_trabajador;
create policy disponibilidad_lectura on public.disponibilidad_trabajador
 for select to authenticated using (activo = true or trabajador_id = auth.uid());
drop policy if exists disponibilidad_propias_all on public.disponibilidad_trabajador;
create policy disponibilidad_propias_all on public.disponibilidad_trabajador
 for all to authenticated using (trabajador_id = auth.uid()) with check (trabajador_id = auth.uid());

drop policy if exists conversaciones_participantes_select on public.conversaciones;
create policy conversaciones_participantes_select on public.conversaciones
 for select to authenticated using (
   exists (select 1 from public.solicitudes s
     where s.id = solicitud_id and auth.uid() in (s.cliente_id, s.trabajador_id))
 );
drop policy if exists conversaciones_participantes_insert on public.conversaciones;
create policy conversaciones_participantes_insert on public.conversaciones
 for insert to authenticated with check (
   exists (select 1 from public.solicitudes s
     where s.id = solicitud_id and auth.uid() in (s.cliente_id, s.trabajador_id))
 );

drop policy if exists mensajes_participantes_select on public.mensajes;
create policy mensajes_participantes_select on public.mensajes
 for select to authenticated using (
   exists (select 1 from public.conversaciones c join public.solicitudes s on s.id=c.solicitud_id
     where c.id=conversacion_id and auth.uid() in (s.cliente_id,s.trabajador_id))
 );
drop policy if exists mensajes_participantes_insert on public.mensajes;
create policy mensajes_participantes_insert on public.mensajes
 for insert to authenticated with check (
   remitente_id = auth.uid() and exists (
     select 1 from public.conversaciones c join public.solicitudes s on s.id=c.solicitud_id
     where c.id=conversacion_id and auth.uid() in (s.cliente_id,s.trabajador_id)
   )
 );

-- Los mensajes no se editan desde el cliente; para marcar lectura se usa RPC.

drop policy if exists resenas_lectura on public.resenas;
create policy resenas_lectura on public.resenas
 for select to authenticated using (true);
drop policy if exists resenas_insert_autorizado on public.resenas;
create policy resenas_insert_autorizado on public.resenas
 for insert to authenticated with check (
   autor_id = auth.uid()
   and exists (
     select 1 from public.solicitudes s
     where s.id=solicitud_id and s.estado='Completada'
       and auth.uid() in (s.cliente_id,s.trabajador_id)
       and destinatario_id = case when auth.uid()=s.cliente_id then s.trabajador_id else s.cliente_id end
   )
 );

drop policy if exists notificaciones_propias_select on public.notificaciones;
create policy notificaciones_propias_select on public.notificaciones
 for select to authenticated using (usuario_id=auth.uid());
drop policy if exists notificaciones_propias_update on public.notificaciones;
create policy notificaciones_propias_update on public.notificaciones
 for update to authenticated using (usuario_id=auth.uid()) with check (usuario_id=auth.uid());
-- No se permite que el cliente cree notificaciones para otras cuentas.

-- Solo los participantes pueden crear/consultar el canal de una solicitud.
create or replace function public.asegurar_conversacion_solicitud(p_solicitud_id uuid)
returns uuid language plpgsql security definer set search_path=public as $$
declare v_id uuid;
begin
 if auth.uid() is null then raise exception 'Sesión no válida'; end if;
 if not exists (select 1 from public.solicitudes s where s.id=p_solicitud_id
                and auth.uid() in (s.cliente_id,s.trabajador_id)) then
   raise exception 'No tienes acceso a esta solicitud';
 end if;
 insert into public.conversaciones(solicitud_id) values(p_solicitud_id)
 on conflict(solicitud_id) do update set actualizada_en=now()
 returning id into v_id;
 return v_id;
end $$;
create or replace function public.marcar_mensajes_leidos(p_conversacion_id uuid)
returns void language plpgsql security definer set search_path=public as $$
begin
 if auth.uid() is null then raise exception 'Sesión no válida'; end if;
 if not exists (
   select 1 from public.conversaciones c join public.solicitudes s on s.id=c.solicitud_id
   where c.id=p_conversacion_id and auth.uid() in (s.cliente_id,s.trabajador_id)
 ) then raise exception 'No tienes acceso a esta conversación'; end if;
 update public.mensajes set leido_en=now()
 where conversacion_id=p_conversacion_id and remitente_id<>auth.uid() and leido_en is null;
end $$;
revoke all on function public.marcar_mensajes_leidos(uuid) from public;
grant execute on function public.marcar_mensajes_leidos(uuid) to authenticated;

revoke all on function public.asegurar_conversacion_solicitud(uuid) from public;
grant execute on function public.asegurar_conversacion_solicitud(uuid) to authenticated;

commit;
