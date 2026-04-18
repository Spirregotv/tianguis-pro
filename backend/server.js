/**
 * server.js – Backend Express para Tianguis Pro
 * Desplegado en Render.com como Web Service
 *
 * ════════════════════════════════════════════════════════════════
 * CÓMO DESPLEGAR EN RENDER.COM (gratis, sin tarjeta):
 *
 *  1. Sube este proyecto a GitHub (solo la carpeta backend/)
 *     O sube todo el repositorio y apunta a la carpeta backend/
 *
 *  2. Ve a https://render.com → New → Web Service
 *
 *  3. Conecta tu repositorio de GitHub
 *
 *  4. Configura:
 *       Name:        tianguis-pro-backend
 *       Root Dir:    backend          ← carpeta donde está este archivo
 *       Runtime:     Node
 *       Build Cmd:   npm install
 *       Start Cmd:   node server.js
 *       Plan:        Free
 *
 *  5. En "Environment Variables" agrega:
 *       Key:   MP_ACCESS_TOKEN
 *       Value: TEST-8378431719832220-041813-5daaf0af15add0d85c37dd943e3ce1ec-1388156734
 *
 *  6. Clic en "Create Web Service"
 *     → Render te dará una URL como: https://tianguis-pro-backend.onrender.com
 *
 *  7. Copia esa URL y pégala en PreferenceApiClient.java → BACKEND_URL
 * ════════════════════════════════════════════════════════════════
 *
 * ⚠ NOTA – Plan Gratuito de Render:
 *   El servidor se "duerme" después de 15 min sin tráfico.
 *   La primera petición tarda ~30 seg en despertar. Las siguientes son rápidas.
 *   Para producción real usa el plan Starter ($7/mes) o migra a Firebase Blaze.
 */

const express = require("express");
const { MercadoPagoConfig, Preference } = require("mercadopago");

const app = express();
app.use(express.json());

// ── El Access Token se lee de la variable de entorno de Render ───
// NUNCA hardcodees el token de producción en este archivo
const accessToken = process.env.MP_ACCESS_TOKEN;

if (!accessToken) {
  console.error("ERROR: MP_ACCESS_TOKEN no está configurado como variable de entorno.");
  process.exit(1);
}

const client = new MercadoPagoConfig({ accessToken });

// ── Health check (Render lo usa para saber si el server está vivo) ──
app.get("/", (req, res) => {
  res.json({ status: "ok", app: "Tianguis Pro Backend" });
});

/**
 * POST /createPreference
 *
 * Body esperado desde Android:
 * {
 *   "totalPrice": 150.00,
 *   "items": [
 *     { "name": "Producto", "quantity": 2, "price": 75.00 }
 *   ]
 * }
 *
 * Respuesta a Android:
 * { "id": "preference_id" }
 */
app.post("/createPreference", async (req, res) => {
  const { totalPrice, items } = req.body;

  // Validación básica
  if (!totalPrice || typeof totalPrice !== "number" || totalPrice <= 0) {
    return res.status(400).json({ error: "totalPrice debe ser un número mayor a 0" });
  }

  // Construir ítems para MP
  const mpItems = Array.isArray(items) && items.length > 0
    ? items.map((item) => ({
        title: item.name || "Producto",
        quantity: Number(item.quantity) || 1,
        unit_price: Number(item.price) || 0,
        currency_id: "MXN",
      }))
    : [{ title: "Venta de Tianguis", quantity: 1, unit_price: totalPrice, currency_id: "MXN" }];

  try {
    const preference = new Preference(client);
    const result = await preference.create({
      body: {
        items: mpItems,
        back_urls: {
          success: "https://example.com/success",
          failure: "https://example.com/failure",
          pending: "https://example.com/pending",
        },
        auto_return: "approved",
        expiration_date_to: new Date(Date.now() + 30 * 60 * 1000).toISOString(),
      },
    });

    // Devuelve solo el ID que necesita Android
    return res.json({ id: result.id });

  } catch (error) {
    console.error("Error creando preferencia:", error.message);
    return res.status(500).json({ error: "No se pudo crear la preferencia: " + error.message });
  }
});

// Render asigna el puerto automáticamente via process.env.PORT
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Tianguis Pro Backend corriendo en puerto ${PORT}`);
});
