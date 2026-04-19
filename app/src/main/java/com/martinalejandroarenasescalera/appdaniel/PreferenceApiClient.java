package com.martinalejandroarenasescalera.appdaniel;

import android.os.Handler;
import android.os.Looper;

import com.martinalejandroarenasescalera.appdaniel.model.CartItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PreferenceApiClient – Llama al backend en Render.com para crear
 * una preferencia de pago en Mercado Pago de forma segura.
 *
 * El Access Token NUNCA sale de este código Android;
 * vive únicamente en la variable de entorno del servidor Render.
 *
 * ════════════════════════════════════════════════════════════════
 * CONFIGURACIÓN:
 *   Después de desplegar en Render, reemplaza BACKEND_URL con
 *   la URL que te asignó Render, por ejemplo:
 *   "https://tianguis-pro-backend.onrender.com"
 * ════════════════════════════════════════════════════════════════
 */
public class PreferenceApiClient {

    // ── Reemplaza esta URL con la de tu Web Service en Render ────
    // Render te la muestra en el dashboard al terminar el deploy,
    // formato: https://NOMBRE-DE-TU-SERVICIO.onrender.com
    private static final String BACKEND_URL =
            "https://tianguis-pro.onrender.com/createPreference";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        /** Devuelve la URL de checkout de Mercado Pago para abrir en el navegador. */
        void onSuccess(String checkoutUrl);
        void onError(String message);
    }

    /**
     * Crea una preferencia de pago con un monto y descripción simples.
     * Usado por el generador de QR para cobros presenciales.
     */
    public void createPreferenceForAmount(double amount, String description, Callback callback) {
        executor.execute(() -> {
            try {
                JSONArray jsonItems = new JSONArray();
                JSONObject jsonItem = new JSONObject();
                jsonItem.put("name",     description);
                jsonItem.put("quantity", 1);
                jsonItem.put("price",    amount);
                jsonItems.put(jsonItem);

                JSONObject body = new JSONObject();
                body.put("totalPrice", amount);
                body.put("items", jsonItems);

                HttpURLConnection conn = (HttpURLConnection) new URL(BACKEND_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(70_000);
                conn.setReadTimeout(70_000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int statusCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        statusCode < 400 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                if (statusCode >= 200 && statusCode < 300) {
                    JSONObject json = new JSONObject(sb.toString());
                    String checkoutUrl = json.optString("sandbox_init_point", "");
                    if (checkoutUrl.isEmpty()) checkoutUrl = json.optString("init_point", "");
                    final String url = checkoutUrl;
                    mainHandler.post(() -> callback.onSuccess(url));
                } else {
                    String errorMsg = "Error del servidor (" + statusCode + "): " + sb;
                    mainHandler.post(() -> callback.onError(errorMsg));
                }
            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Error de red";
                mainHandler.post(() -> callback.onError(msg));
            }
        });
    }

    /**
     * Envía los ítems del carrito al backend y recibe el preference_id.
     * La llamada HTTP ocurre en un hilo de fondo; el resultado llega al Main Thread.
     */
    public void createPreference(List<CartItem> items, Callback callback) {
        executor.execute(() -> {
            try {
                // ── Construir JSON del cuerpo ────────────────────
                JSONArray jsonItems = new JSONArray();
                for (CartItem item : items) {
                    JSONObject jsonItem = new JSONObject();
                    jsonItem.put("name",     item.getProduct().getName());
                    jsonItem.put("quantity", item.getQuantity());
                    jsonItem.put("price",    item.getProduct().getPrice());
                    jsonItems.put(jsonItem);
                }

                double total = 0;
                for (CartItem item : items) total += item.getSubtotal();

                JSONObject body = new JSONObject();
                body.put("totalPrice", total);
                body.put("items", jsonItems);

                // ── Petición HTTP POST al backend de Render ──────
                HttpURLConnection conn = (HttpURLConnection) new URL(BACKEND_URL).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(70_000);
                conn.setReadTimeout(70_000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int statusCode = conn.getResponseCode();

                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        statusCode < 400 ? conn.getInputStream() : conn.getErrorStream()
                ));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                if (statusCode >= 200 && statusCode < 300) {
                    JSONObject json = new JSONObject(sb.toString());
                    String checkoutUrl = json.optString("sandbox_init_point", "");
                    if (checkoutUrl.isEmpty()) {
                        checkoutUrl = json.optString("init_point", "");
                    }
                    final String url = checkoutUrl;
                    mainHandler.post(() -> callback.onSuccess(url));
                } else {
                    String errorMsg = "Error del servidor (" + statusCode + "): " + sb;
                    mainHandler.post(() -> callback.onError(errorMsg));
                }

            } catch (Exception e) {
                String msg = e.getMessage() != null ? e.getMessage() : "Error de red";
                mainHandler.post(() -> callback.onError(msg));
            }
        });
    }
}
