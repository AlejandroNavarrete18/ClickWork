// Edge Function: crear-pago-servicio
// El cliente pide pagar una solicitud. El servidor decide el monto y crea la
// preferencia de Checkout Pro en Mercado Pago. El APK solo recibe la URL a abrir.
//
// Secretos requeridos (supabase secrets set ...):
//   MP_ACCESS_TOKEN        token privado de Mercado Pago (de pruebas o producción)
// Opcionales:
//   MP_RETORNO_URL         URL https a la que Mercado Pago regresa al cliente (opcional)
// Supabase inyecta solas: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const CORS = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...CORS, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: CORS });
  if (req.method !== "POST") return json({ error: "Método no permitido" }, 405);

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const mpToken = Deno.env.get("MP_ACCESS_TOKEN");
  if (!mpToken) return json({ error: "Pagos no configurados en el servidor" }, 500);

  // 1. Quién es el usuario (JWT validado por Supabase Auth, no por lo que diga el celular)
  const authHeader = req.headers.get("Authorization") ?? "";
  const userClient = createClient(supabaseUrl, anonKey, {
    global: { headers: { Authorization: authHeader } },
  });
  const { data: userData, error: userErr } = await userClient.auth.getUser();
  if (userErr || !userData.user) return json({ error: "Sesión no válida" }, 401);
  const userId = userData.user.id;

  let solicitudId = "";
  try {
    const body = await req.json();
    solicitudId = String(body.solicitud_id ?? "");
  } catch {
    return json({ error: "Solicitud inválida" }, 400);
  }
  if (!/^[0-9a-f-]{36}$/i.test(solicitudId)) return json({ error: "Solicitud inválida" }, 400);

  const admin = createClient(supabaseUrl, serviceKey);

  // 2. La solicitud y el precio salen de la base de datos
  const { data: sol, error: solErr } = await admin
    .from("solicitudes")
    .select("id, cliente_id, trabajador_id, servicio, estado, precio_cotizado")
    .eq("id", solicitudId)
    .maybeSingle();
  if (solErr || !sol) return json({ error: "Solicitud no encontrada" }, 404);
  if (sol.cliente_id !== userId) return json({ error: "Solo el cliente puede pagar esta solicitud" }, 403);
  if (!["Confirmada", "En curso", "Completada"].includes(sol.estado)) {
    return json({ error: "La solicitud aún no está lista para pago" }, 409);
  }
  const monto = Number(sol.precio_cotizado);
  if (!Number.isFinite(monto) || monto <= 0) return json({ error: "La solicitud no tiene precio" }, 409);

  // 3. No cobrar dos veces
  const { data: yaPagado } = await admin
    .from("pagos_servicio")
    .select("id")
    .eq("solicitud_id", solicitudId)
    .in("estado", ["aprobado", "confirmado", "por_confirmar"])
    .limit(1)
    .maybeSingle();
  if (yaPagado) return json({ error: "Esta solicitud ya tiene un pago registrado" }, 409);

  // 4. Registro del intento (pendiente). Su id viaja como external_reference.
  const { data: pago, error: pagoErr } = await admin
    .from("pagos_servicio")
    .insert({
      solicitud_id: sol.id,
      cliente_id: sol.cliente_id,
      trabajador_id: sol.trabajador_id,
      monto_mxn: monto,
    })
    .select("id")
    .single();
  if (pagoErr || !pago) return json({ error: "No se pudo iniciar el pago" }, 500);

  // 5. Preferencia de Checkout Pro
  const retorno = Deno.env.get("MP_RETORNO_URL");
  const preferencia: Record<string, unknown> = {
    items: [{
      id: sol.id,
      title: `Servicio: ${String(sol.servicio).slice(0, 80)}`,
      quantity: 1,
      unit_price: monto,
      currency_id: "MXN",
    }],
    external_reference: pago.id,
    notification_url: `${supabaseUrl}/functions/v1/mercadopago-webhook`,
    statement_descriptor: "CLICKWORK",
    expires: true,
    // 72 h: pagos en OXXO o SPEI pueden tardar en acreditarse.
    expiration_date_to: new Date(Date.now() + 72 * 60 * 60 * 1000).toISOString(),
  };
  if (retorno) {
    preferencia.back_urls = { success: retorno, failure: retorno, pending: retorno };
    preferencia.auto_return = "approved";
  }

  const mpRes = await fetch("https://api.mercadopago.com/checkout/preferences", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${mpToken}`,
      "Content-Type": "application/json",
      "X-Idempotency-Key": pago.id,
    },
    body: JSON.stringify(preferencia),
  });
  const mpJson = await mpRes.json().catch(() => ({}));
  if (!mpRes.ok || !mpJson.init_point) {
    console.error("Mercado Pago preferencia falló", mpRes.status, JSON.stringify(mpJson));
    await admin.from("pagos_servicio").update({ estado: "cancelado", updated_at: new Date().toISOString() }).eq("id", pago.id);
    return json({ error: "Mercado Pago no pudo crear el cobro. Inténtalo de nuevo." }, 502);
  }

  await admin.from("pagos_servicio")
    .update({ preferencia_id: String(mpJson.id), updated_at: new Date().toISOString() })
    .eq("id", pago.id);

  return json({ pago_id: pago.id, url: mpJson.init_point });
});
