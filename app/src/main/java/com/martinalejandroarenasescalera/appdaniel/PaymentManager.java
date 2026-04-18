package com.martinalejandroarenasescalera.appdaniel;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

/**
 * PaymentManager – Abre el checkout de Mercado Pago en el navegador del dispositivo.
 *
 * Flujo:
 *  1. El backend Render crea la preferencia y devuelve sandbox_init_point / init_point.
 *  2. startCheckout() abre esa URL en el navegador predeterminado del usuario.
 *  3. El usuario completa el pago en el navegador y regresa a la app.
 *  4. CartActivity detecta el regreso en onResume() y muestra un diálogo de confirmación.
 *
 * Modo TEST  → usa sandbox_init_point  (PreferenceApiClient ya selecciona la URL correcta)
 * Producción → usa init_point
 */
public class PaymentManager {

    /**
     * Abre la URL de checkout de Mercado Pago en el navegador del dispositivo.
     *
     * @param activity     Activity desde donde se lanza el checkout
     * @param checkoutUrl  URL devuelta por el backend (sandbox_init_point o init_point)
     */
    public static void startCheckout(Activity activity, String checkoutUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl));
        activity.startActivity(intent);
    }

    // ── Clase de resultado ───────────────────────────────────────

    public static class PaymentResult {
        /** true si el usuario confirmó que el pago fue aprobado. */
        public final boolean approved;
        /** ID simbólico del pago (el pago real se confirma via webhook en producción). */
        public final String paymentId;
        /** Estado: "approved", "cancelled". */
        public final String status;

        public PaymentResult(boolean approved, String paymentId, String status) {
            this.approved = approved;
            this.paymentId = paymentId;
            this.status = status != null ? status : "";
        }

        public boolean isCancelled() { return "cancelled".equals(status); }
    }
}
