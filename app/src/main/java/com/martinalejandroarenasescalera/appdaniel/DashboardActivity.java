package com.martinalejandroarenasescalera.appdaniel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.martinalejandroarenasescalera.appdaniel.model.SaleRecord;
import com.martinalejandroarenasescalera.appdaniel.repository.ProductRepository;

import java.util.List;

/**
 * DashboardActivity – Panel principal del vendedor.
 * Muestra un resumen de las ventas del día:
 *   • Total en MXN recaudado hoy
 *   • Número de transacciones completadas
 * Desde aquí se navega al Inventario y al Generador de QR.
 */
public class DashboardActivity extends AppCompatActivity {

    private TextView tvTotalSales, tvTransactions, tvWelcome;
    private ProductRepository repository;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // Sesión expirada, regresar al login
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        repository = ProductRepository.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvWelcome = findViewById(R.id.tvWelcome);
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvTransactions = findViewById(R.id.tvTransactions);

        MaterialButton btnInventory = findViewById(R.id.btnInventory);
        MaterialButton btnQR = findViewById(R.id.btnQR);
        MaterialButton btnLogout = findViewById(R.id.btnLogout);

        // Mostrar correo del vendedor
        tvWelcome.setText("Bienvenido,\n" + currentUser.getEmail());

        // Cargar resumen de ventas del día
        loadTodayStats();

        btnInventory.setOnClickListener(v ->
                startActivity(new Intent(this, AdminActivity.class)));

        btnQR.setOnClickListener(v ->
                startActivity(new Intent(this, QRGeneratorActivity.class)));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recargar estadísticas al volver de otras pantallas
        loadTodayStats();
    }

    private void loadTodayStats() {
        repository.loadTodaySales(currentUser.getUid(), new ProductRepository.OnSalesLoadedListener() {
            @Override
            public void onLoaded(List<SaleRecord> sales) {
                double total = 0;
                int approvedCount = 0;
                for (SaleRecord sale : sales) {
                    if ("approved".equalsIgnoreCase(sale.getStatus())) {
                        total += sale.getTotal();
                        approvedCount++;
                    }
                }
                tvTotalSales.setText(String.format("$%.2f", total));
                tvTransactions.setText(String.valueOf(approvedCount));
            }

            @Override
            public void onError(String errorMessage) {
                tvTotalSales.setText("$0.00");
                tvTransactions.setText("0");
            }
        });
    }
}
