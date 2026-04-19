package com.martinalejandroarenasescalera.appdaniel;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/**
 * QRGeneratorActivity – Generador de códigos QR para cobros presenciales.
 *
 * Crea una preferencia de pago en Mercado Pago a través del backend,
 * y genera un QR con la URL de checkout. El cliente escanea el QR
 * y se abre la app de Mercado Pago para completar el pago.
 */
public class QRGeneratorActivity extends AppCompatActivity {

    private static final int QR_SIZE = 600;

    private TextInputEditText etAmount, etDescription;
    private ImageView imgQR;
    private TextView tvAmountDisplay;
    private LinearLayout layoutQRResult;
    private ProgressBar progressBar;
    private Button btnGenerate;

    private final PreferenceApiClient apiClient = new PreferenceApiClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_generator);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.qr_title);
        }

        etAmount      = findViewById(R.id.etAmount);
        etDescription = findViewById(R.id.etDescription);
        imgQR         = findViewById(R.id.imgQR);
        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        layoutQRResult  = findViewById(R.id.layoutQRResult);
        progressBar     = findViewById(R.id.progressBar);
        btnGenerate     = findViewById(R.id.btnGenerateQR);
        Button btnNew   = findViewById(R.id.btnNewQR);

        btnGenerate.setOnClickListener(v -> generateQR());
        btnNew.setOnClickListener(v -> resetForm());
    }

    private void generateQR() {
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(description)) {
            description = "Cobro Tianguis Pro";
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        // Mostrar carga y deshabilitar botón mientras se crea la preferencia
        progressBar.setVisibility(View.VISIBLE);
        btnGenerate.setEnabled(false);
        etAmount.setEnabled(false);
        etDescription.setEnabled(false);

        final String desc = description;
        final double finalAmount = amount;

        apiClient.createPreferenceForAmount(amount, description, new PreferenceApiClient.Callback() {
            @Override
            public void onSuccess(String checkoutUrl) {
                progressBar.setVisibility(View.GONE);
                Bitmap qrBitmap = generateQRBitmap(checkoutUrl);
                if (qrBitmap != null) {
                    imgQR.setImageBitmap(qrBitmap);
                    tvAmountDisplay.setText(String.format("$%.2f MXN — %s", finalAmount, desc));
                    layoutQRResult.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                btnGenerate.setEnabled(true);
                etAmount.setEnabled(true);
                etDescription.setEnabled(true);
                Toast.makeText(QRGeneratorActivity.this,
                        "Error al crear cobro: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Bitmap generateQRBitmap(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            Bitmap bitmap = Bitmap.createBitmap(QR_SIZE, QR_SIZE, Bitmap.Config.RGB_565);
            for (int x = 0; x < QR_SIZE; x++) {
                for (int y = 0; y < QR_SIZE; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bitmap;
        } catch (WriterException e) {
            Toast.makeText(this, "Error generando QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void resetForm() {
        etAmount.setText("");
        etDescription.setText("");
        etAmount.setEnabled(true);
        etDescription.setEnabled(true);
        btnGenerate.setEnabled(true);
        imgQR.setImageDrawable(null);
        layoutQRResult.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
