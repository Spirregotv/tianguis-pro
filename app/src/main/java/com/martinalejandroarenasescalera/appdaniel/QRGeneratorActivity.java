package com.martinalejandroarenasescalera.appdaniel;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
 * Genera un código QR con la información del cobro para que el cliente
 * lo escanee con su app de Mercado Pago o cualquier lector de QR.
 *
 * ═══════════════════════════════════════════════════════════════
 * INTEGRACIÓN CON MERCADO PAGO QR:
 *
 * Para cobros QR reales con Mercado Pago ("Cobro con QR Point of Sale"):
 *  1. Crea una caja (POS) en el Panel de Mercado Pago
 *  2. Genera un "fixed QR" para tu caja desde el API o el panel
 *  3. O usa el API de Órdenes para crear QR dinámicos con monto específico
 *
 * Referencia: https://www.mercadopago.com.mx/developers/es/docs/qr-code
 *
 * En esta implementación, el QR contiene el monto y datos del vendedor
 * como texto simple (útil para integración manual o QR informativos).
 * ═══════════════════════════════════════════════════════════════
 */
public class QRGeneratorActivity extends AppCompatActivity {

    private static final int QR_SIZE = 600; // píxeles

    private TextInputEditText etAmount;
    private ImageView imgQR;
    private TextView tvAmountDisplay, tvInstruction;
    private LinearLayout layoutQRResult;

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

        etAmount = findViewById(R.id.etAmount);
        imgQR = findViewById(R.id.imgQR);
        tvAmountDisplay = findViewById(R.id.tvAmountDisplay);
        tvInstruction = findViewById(R.id.tvInstruction);
        layoutQRResult = findViewById(R.id.layoutQRResult);
        Button btnGenerate = findViewById(R.id.btnGenerateQR);
        Button btnNew = findViewById(R.id.btnNewQR);

        btnGenerate.setOnClickListener(v -> generateQR());
        btnNew.setOnClickListener(v -> resetForm());
    }

    private void generateQR() {
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

        if (TextUtils.isEmpty(amountStr)) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show();
            return;
        }

        // Contenido del QR: JSON con monto, moneda y timestamp
        String qrContent = String.format(
                "{\"app\":\"TianguisPro\",\"amount\":%.2f,\"currency\":\"MXN\",\"ts\":%d}",
                amount, System.currentTimeMillis()
        );

        Bitmap qrBitmap = generateQRBitmap(qrContent);
        if (qrBitmap != null) {
            imgQR.setImageBitmap(qrBitmap);
            tvAmountDisplay.setText(String.format("$%.2f MXN", amount));
            layoutQRResult.setVisibility(View.VISIBLE);
            etAmount.setEnabled(false);
            findViewById(R.id.btnGenerateQR).setVisibility(View.GONE);
        }
    }

    /**
     * Genera el Bitmap del código QR usando la librería ZXing.
     *
     * @param content Texto/JSON a codificar en el QR
     * @return Bitmap del QR en blanco y negro, o null si hay error
     */
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
        etAmount.setEnabled(true);
        imgQR.setImageDrawable(null);
        layoutQRResult.setVisibility(View.GONE);
        findViewById(R.id.btnGenerateQR).setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
