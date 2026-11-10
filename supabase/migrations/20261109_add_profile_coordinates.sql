-- ClickWork Live Map: optional geolocation for worker profiles.
-- Run this migration in the Supabase SQL Editor before publishing worker pins.
ALTER TABLE public.perfiles
    ADD COLUMN IF NOT EXISTS latitud double precision,
    ADD COLUMN IF NOT EXISTS longitud double precision;
