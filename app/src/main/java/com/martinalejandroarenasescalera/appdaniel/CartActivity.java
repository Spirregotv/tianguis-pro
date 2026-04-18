package com.martinalejandroarenasescalera.appdaniel;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.martinalejandroarenasescalera.appdaniel.adapter.CartAdapter;
import com.martinalejandroarenasescalera.appdaniel.model.CartItem;
import com.martinalejandroarenasescalera.appdaniel.model.SaleRecord;
import com.martinalejandroarenasescalera.appdaniel.repository.ProductRepository;

/**
 * CartActivity – Carrito de compras del cliente.
 *
 * Flujo de pago:
 *  1. Calcula el total del carrito
 *  2. Llama al backend Render "createPreference" con el total y los ítems
 *  3. El backend crea la preferencia en Mercado Pago y devuelve sandbox_init_point
 *  4. Abre el navegador con esa URL usando PaymentManager
 *  5. Al regresar a la app, onResume() muestra un diálogo de confirmación
 *  6. Si el usuario confirma: descuenta stock en Firebase y registra la venta
 */
public class CartActivity extends AppCompatActivity implements CartAdapter.OnCartChangeListener {

    private CartAdapter adapter;
    private TextView tvTotal, tvEmpty;
    private RecyclerView recyclerView;
    private Button btnPay, btnClear;
    private ProgressBar progressBar;

    /** true mientras el checkout está abierto en el navegador. */
    private boolean paymentInitiated = false;

    private final PreferenceApiClient preferenceApiClient = new PreferenceApiClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.cart_title);
        }

        tvTotal = findViewById(R.id.tvTotal);
        tvEmpty = findViewById(R.id.tvCartEmpty);
        recyclerView = findViewById(R.id.recyclerCart);
        btnPay = findViewById(R.id.btnPayNow);
        btnClear = findViewById(R.id.btnClearCart);
        progressBar = findViewById(R.id.progressBar);

        adapter = new CartAdapter(ClientActivity.cart, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        updateTotal();

        btnPay.setOnClickListener(v -> realizarPago());
        btnClear.setOnClickListener(v -> clearCart());
    }

    /**
     * Paso 1: Llama a PreferenceApiClient para crear la preferencia en MP
     * y obtener la URL de checkout del backend Render.
     */
    private void realizarPago() {
        if (ClientActivity.cart.isEmpty()) {
            Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        setPaymentUiLoading(true);

        preferenceApiClient.createPreference(ClientActivity.cart, new PreferenceApiClient.Callback() {
            @Override
            public void onSuccess(String checkoutUrl) {
                setPaymentUiLoading(false);
                // Paso 2: abrir el checkout de MP en el navegador
                paymentInitiated = true;
                PaymentManager.startCheckout(CartActivity.this, checkoutUrl);
            }

            @Override
            public void onError(String message) {
                setPaymentUiLoading(false);
                Toast.makeText(CartActivity.this,
                        "No se pudo iniciar el pago: " + message,
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Paso 3: El usuario regresa a la app desde el navegador.
     * Mostramos un diálogo de confirmación para saber si el pago fue exitoso.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (paymentInitiated) {
            paymentInitiated = false;
            mostrarDialogoConfirmacion();
        }
    }

    private void mostrarDialogoConfirmacion() {
        new AlertDialog.Builder(this)
                .setTitle("Confirmación de pago")
                .setMessage("¿Se completó tu pago en Mercado Pago?")
                .setPositiveButton("Sí, fue aprobado", (dialog, which) ->
                        onPaymentApproved("browser-checkout"))
                .setNegativeButton("No / Cancelé", (dialog, which) ->
                        Toast.makeText(this, R.string.payment_cancelled, Toast.LENGTH_SHORT).show())
                .setCancelable(false)
                .show();
    }

    /**
     * Pago confirmado → descontar stock en Firebase y registrar la venta.
     */
    private void onPaymentApproved(String paymentId) {
        double total = calculateTotal();
        int itemCount = ClientActivity.cart.size();
        ProductRepository repo = ProductRepository.getInstance();

        // Descontar stock de cada producto vendido
        for (CartItem item : ClientActivity.cart) {
            repo.decrementStock(item.getProduct().getId(), item.getQuantity());
        }

        // Guardar registro de venta (solo si hay vendedor autenticado)
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            SaleRecord sale = new SaleRecord(total, itemCount, paymentId, "approved");
            repo.saveSale(user.getUid(), sale, new ProductRepository.OnCompleteListener() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String errorMessage) {}
            });
        }

        // Limpiar el carrito y volver al catálogo
        ClientActivity.cart.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, R.string.payment_success, Toast.LENGTH_LONG).show();
        updateTotal();
        finish();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void clearCart() {
        ClientActivity.cart.clear();
        adapter.notifyDataSetChanged();
        updateTotal();
    }

    @Override
    public void onQuantityChanged() { updateTotal(); }

    @Override
    public void onItemRemoved(int position) { updateTotal(); }

    private void updateTotal() {
        boolean isEmpty = ClientActivity.cart.isEmpty();
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        btnPay.setEnabled(!isEmpty);
        tvTotal.setText(String.format("Total: $%.2f MXN", calculateTotal()));
    }

    private double calculateTotal() {
        double total = 0;
        for (CartItem item : ClientActivity.cart) total += item.getSubtotal();
        return total;
    }

    /** Bloquea la UI mientras se espera la respuesta del backend. */
    private void setPaymentUiLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnPay.setEnabled(!loading);
        btnClear.setEnabled(!loading);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
