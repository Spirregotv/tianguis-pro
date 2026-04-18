/**
 * Firebase Cloud Functions – Tianguis Pro
 * Archivo: functions/index.js
 *
 * ════════════════════════════════════════════════════════════════
 * CÓMO DESPLEGAR ESTAS FUNCIONES:
 *
 *  1. Instala Firebase CLI (una sola vez):
 *       npm install -g firebase-tools
 *
 *  2. En la carpeta raíz del proyecto Firebase (donde está firebase.json):
 *       firebase login
 *       firebase init functions   ← elige "Use existing project"
 *
 *  3. Instala dependencias:
 *       cd functions
 *       npm install
 *
 *  4. Despliega:
 *       firebase deploy --only functions
 *
 *  5. Verifica que la función aparezca en:
 *       https://console.firebase.google.com/ → Functions
 * ════════════════════════════════════════════════════════════════
 *
 * ⚠ SEGURIDAD – ACCESS TOKEN:
 *   Para PRODUCCIÓN, NUNCA dejes el token hardcodeado aquí.
 *   Usa Firebase Secret Manager:
 *     firebase functions:secrets:set MP_ACCESS_TOKEN
 *   Y luego en el código:
 *     const accessToken = process.env.MP_ACCESS_TOKEN;
 *   Con la anotación: runWith({ secrets: ["MP_ACCESS_TOKEN"] })
 *
 *   El token TEST de abajo es solo para pruebas; no da acceso a dinero real.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { MercadoPagoConfig, Preference } = require("mercadopago");

// ── Credencial del servidor (SOLO aquí, nunca en Android) ────────
// Para Producción: cambia "TEST-..." por tu "APP_USR-..." de producción
const client = new MercadoPagoConfig({
  accessToken: "TEST-8378431719832220-041813-5daaf0af15add0d85c37dd943e3ce1ec-1388156734",
});

/**
 * createPreference – Crea una preferencia de pago en Mercado Pago.
 *
 * Recibe desde Android:
 *   data.totalPrice  {number}  Total de la venta en MXN
 *   data.items       {Array}   (opcional) Lista de productos del carrito
 *
 * Devuelve a Android:
 *   { id: "preference_id" }  ← este ID es el que usa el SDK de Android
 */
exports.createPreference = onCall(async (request) => {
  const data = request.data;

  // Validación básica del precio
  if (!data.totalPrice || typeof data.totalPrice !== "number" || data.totalPrice <= 0) {
    throw new HttpsError("invalid-argument", "totalPrice debe ser un número mayor a 0");
  }

  // Armar los ítems: se puede pasar el detalle del carrito o usar un ítem genérico
  const items = Array.isArray(data.items) && data.items.length > 0
    ? data.items.map((item) => ({
        title: item.name || "Producto",
        quantity: item.quantity || 1,
        unit_price: item.price || 0,
        currency_id: "MXN",
      }))
    : [
        {
          title: "Venta de Tianguis",
          quantity: 1,
          unit_price: data.totalPrice,
          currency_id: "MXN",
        },
      ];

  try {
    const preference = new Preference(client);
    const result = await preference.create({
      body: {
        items,
        back_urls: {
          // Estas URLs deben existir; para pruebas puedes usar cualquier URL válida
          success: "https://tu-dominio.com/pago/exito",
          failure: "https://tu-dominio.com/pago/error",
          pending: "https://tu-dominio.com/pago/pendiente",
        },
        auto_return: "approved",
        // Tiempo de expiración: 30 minutos para que el cliente pague
        expiration_date_to: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
      },
    });

    // Retorna solo el ID; Android lo usa para abrir el checkout
    return { id: result.id };

  } catch (error) {
    console.error("Error creando preferencia MP:", error);
    throw new HttpsError("internal", "No se pudo crear la preferencia de pago: " + error.message);
  }
});
