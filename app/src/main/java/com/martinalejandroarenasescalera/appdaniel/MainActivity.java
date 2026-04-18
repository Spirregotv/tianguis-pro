package com.martinalejandroarenasescalera.appdaniel;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

/**
 * MainActivity – Pantalla de inicio de Tianguis Pro.
 *
 * Muestra dos opciones:
 *  • "Soy Vendedor" → LoginActivity (autenticación Firebase)
 *  • "Ver Catálogo"  → ClientActivity (sin login)
 *
 * Si el vendedor ya tiene sesión activa, se redirige directamente al Dashboard.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Si ya hay una sesión de vendedor activa, ir directo al Dashboard
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        Button btnVendedor = findViewById(R.id.btnVendedor);
        Button btnCliente = findViewById(R.id.btnCliente);

        btnVendedor.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));

        btnCliente.setOnClickListener(v ->
                startActivity(new Intent(this, ClientActivity.class)));
    }
}
