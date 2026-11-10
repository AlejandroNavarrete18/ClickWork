// Edge Function: mercadopago-webhook
// ÚNICO lugar donde un pago puede pasar a 'aprobado'. Flujo:
//   1. Verifica la firma x-signature (HMAC-SHA256 con tu clave secreta de webhooks).
//   2. NO confía en el cuerpo: consulta el pago directo a la API de Mercado Pago.
//   3. Comprueba que monto y moneda coinciden con lo que ClickWork registró.
//   4. Actualiza el estado de forma idempotente.
//
// Despliegue: esta función debe aceptar llamadas sin JWT de usuario:
//   supabase functions deploy mercadopago-webhook --no-verify-jwt
// (La seguridad la da la firma, no el JWT.)
//
// Secretos: MP_ACCESS_TOKEN, MP_WEBHOOK_SECRET

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.4";

const ESTADOS: Record<string, string> = {
  approved: "aprobado",
  rejected: "rechazado",
  cancelled: "cancelado",
  refunded: "reembolsado",
  charged_back: "reembolsado",
  pending: "pendiente",
  in_process: "pendiente",
  authorized: "pendiente",
  in_mediation: "en_revision",
};

function hex(buf: ArrayBuffer): string {
  return [...new Uint8Array(buf)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

function igualesConstante(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let r = 0;
  for (let i = 0; i < a.length; i++) r |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return r === 0;
}

async function firmaValida(req: Request, dataId: string, secreto: string): Promise<boolean> {
  const firma = req.headers.get("x-signature") ?? "";
  const requestId = req.headers.get("x-request-id") ?? "";
  let ts = "";
  let v1 = "";
  for (const parte of firma.split(",")) {
    const [k, v] = parte.split("=", 2).map((x) => x?.trim());
    if (k === "ts") ts = v ?? "";
    if (k === "v1") v1 = v ?? "";
  }
  if (!ts || !v1 || !dataId) return false;

  // Los ids alfanuméricos van en minúsculas en el manifiesto.
  const id = /^[a-z0-9]+$/i.test(dataId) ? dataId.toLowerCase() : dataId;
  const manifiesto = `id:${id};request-id:${requestId};ts:${ts};`;

  const clave = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(secreto),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"],
  );
  const calculada = hex(await crypto.subtle.sign("HMAC", clave, new TextEncoder().encode(manifiesto)));
  if (!igualesConstante(calculada, v1)) return false;

  // Rechaza notificaciones demasiado viejas (anti-replay): 10 minutos.
  const tsMs = Number(ts) > 1e12 ? Number(ts) : Number(ts) * 1000;
  return Number.isFinite(tsMs) && Math.abs(Date.now() - tsMs) < 10 * 60 * 1000;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("ok", { status: 200 });

  const mpToken = Deno.env.get("MP_ACCESS_TOKEN");
  const secreto = Deno.env.get("MP_WEBHOOK_SECRET");
  if (!mpToken || !secreto) {
    console.error("Faltan secretos MP_ACCESS_TOKEN / MP_WEBHOOK_SECRET");
    return new Response("config", { status: 500 });
  }

  const url = new URL(req.url);
  let body: any = {};
  try { body = await req.json(); } catch { /* el id también viene en la query */ }

  const tipo = String(url.searchParams.get("type") ?? body.type ?? body.topic ?? "");
  const dataId = String(url.searchParams.get("data.id") ?? body?.data?.id ?? "");

  if (!(await firmaValida(req, dataId, secreto))) {
    return new Response("firma inválida", { status: 401 });
  }
  // Otros tipos de aviso (merchant_order, etc.) no nos interesan: respondemos 200 para que no reintente.
  if (tipo !== "payment") return new Response("ignorado", { status: 200 });

  const admin = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!);

  // Consulta autoritativa a Mercado Pago (no se usa lo que venga en el aviso).
  const mpRes = await fetch(`https://api.mercadopago.com/v1/payments/${encodeURIComponent(dataId)}`, {
    headers: { Authorization: `Bearer ${mpToken}` },
  });
  if (!mpRes.ok) {
    console.error("No se pudo consultar el pago", dataId, mpRes.status);
    return new Response("reintentar", { status: 500 });
  }
  const mp = await mpRes.json();
  const pagoId = String(mp.external_reference ?? "");
  const estadoMp = String(mp.status ?? "");

  // Registro de auditoría / idempotencia (estado incluido para no perder cambios).
  const eventoId = `payment:${dataId}:${estadoMp}`;
  await admin.from("webhook_eventos").upsert(
    { proveedor: "mercadopago", evento_id: eventoId, tipo: "payment", procesado: false },
    { onConflict: "proveedor,evento_id", ignoreDuplicates: true },
  );

  const { data: pago } = await admin
    .from("pagos_servicio")
    .select("id, solicitud_id, trabajador_id, monto_mxn, estado, proveedor_pago_id")
    .eq("id", pagoId)
    .maybeSingle();

  if (!pago) {
    // No es un pago de servicio de ClickWork (p. ej. una suscripción de empresa).
    await admin.from("webhook_eventos").update({ procesado: true })
      .eq("proveedor", "mercadopago").eq("evento_id", eventoId);
    return new Response("sin pago asociado", { status: 200 });
  }

  const nuevo = ESTADOS[estadoMp] ?? "en_revision";

  // Monto y moneda deben coincidir con lo registrado por el servidor.
  const montoMp = Math.round(Number(mp.transaction_amount) * 100);
  const montoDb = Math.round(Number(pago.monto_mxn) * 100);
  if (nuevo === "aprobado" && (montoMp !== montoDb || mp.currency_id !== "MXN")) {
    console.error("Monto/moneda no coinciden", { pagoId, montoMp, montoDb, moneda: mp.currency_id });
    await admin.from("pagos_servicio")
      .update({ estado: "en_revision", proveedor_pago_id: String(mp.id), updated_at: new Date().toISOString() })
      .eq("id", pago.id).neq("estado", "aprobado");
    return new Response("revisión", { status: 200 });
  }

  // Un pago ya aprobado solo puede pasar a reembolsado (avisos fuera de orden no lo degradan).
  if (pago.estado === "aprobado" && nuevo !== "reembolsado") {
    await admin.from("webhook_eventos").update({ procesado: true })
      .eq("proveedor", "mercadopago").eq("evento_id", eventoId);
    return new Response("ok", { status: 200 });
  }

  const cambios: Record<string, unknown> = {
    estado: nuevo,
    proveedor_pago_id: String(mp.id),
    updated_at: new Date().toISOString(),
  };
  if (nuevo === "aprobado") cambios.pagado_en = mp.date_approved ?? new Date().toISOString();

  const { error: updErr } = await admin.from("pagos_servicio").update(cambios).eq("id", pago.id);

  if (updErr) {
    // 23505 = ya existe otro pago aprobado para esta solicitud (doble pago): se separa para reembolso manual.
    if (updErr.code === "23505") {
      await admin.from("pagos_servicio")
        .update({ estado: "en_revision", proveedor_pago_id: String(mp.id), updated_at: new Date().toISOString() })
        .eq("id", pago.id);
      console.error("DOBLE PAGO: revisar y reembolsar en Mercado Pago", { pagoId, mpId: mp.id });
    } else {
      console.error("No se pudo actualizar el pago", updErr);
      return new Response("reintentar", { status: 500 });
    }
  } else if (nuevo === "aprobado") {
    await admin.from("notificaciones").insert({
      usuario_id: pago.trabajador_id,
      tipo: "solicitud",
      titulo: "El cliente pagó el servicio",
      cuerpo: "El pago fue confirmado por Mercado Pago.",
      referencia_id: pago.solicitud_id,
    });
  }

  await admin.from("webhook_eventos").update({ procesado: true })
    .eq("proveedor", "mercadopago").eq("evento_id", eventoId);
  return new Response("ok", { status: 200 });
});
