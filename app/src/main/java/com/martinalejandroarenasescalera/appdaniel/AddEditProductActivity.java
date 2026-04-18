package com.martinalejandroarenasescalera.appdaniel;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.martinalejandroarenasescalera.appdaniel.model.Product;
import com.martinalejandroarenasescalera.appdaniel.repository.ProductRepository;

/**
 * AddEditProductActivity – Formulario para crear o editar un producto.
 *
 * Si se recibe EXTRA_PRODUCT_ID en el Intent, se comporta como "Editar".
 * Si no, crea un producto nuevo en Firebase.
 *
 * Campos del producto:
 *   • Nombre (obligatorio)
 *   • Precio en MXN (obligatorio)
 *   • Stock (obligatorio)
 *   • URL de imagen (opcional – puede ser URL externa o de Firebase Storage)
 *   • Categoría (opcional)
 *
 * FIREBASE STORAGE (imágenes subidas desde el teléfono):
 *   Para subir fotos desde la galería, implementa:
 *   1. StorageReference ref = FirebaseStorage.getInstance().getReference("products/" + productId);
 *   2. ref.putFile(imageUri) → obtén la URL de descarga con ref.getDownloadUrl()
 *   3. Guarda la URL en el producto
 */
public class AddEditProductActivity extends AppCompatActivity {

    private TextInputEditText etName, etPrice, etStock, etImageUrl, etCategory;
    private ProgressBar progressBar;
    private ProductRepository repository;

    private String editingProductId = null; // null = modo crear

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        repository = ProductRepository.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        etName = findViewById(R.id.etProductName);
        etPrice = findViewById(R.id.etProductPrice);
        etStock = findViewById(R.id.etProductStock);
        etImageUrl = findViewById(R.id.etProductImageUrl);
        etCategory = findViewById(R.id.etProductCategory);
        progressBar = findViewById(R.id.progressBar);
        Button btnSave = findViewById(R.id.btnSave);

        // ── Cargar datos si estamos editando ────────────────────
        editingProductId = getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_ID);
        if (editingProductId != null) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(R.string.edit_product_title);

            etName.setText(getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_NAME));
            etPrice.setText(String.valueOf(getIntent().getDoubleExtra(AdminActivity.EXTRA_PRODUCT_PRICE, 0)));
            etStock.setText(String.valueOf(getIntent().getIntExtra(AdminActivity.EXTRA_PRODUCT_STOCK, 0)));
            etImageUrl.setText(getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_IMAGE));
            etCategory.setText(getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_CATEGORY));
        } else {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(R.string.add_product_title);
        }

        btnSave.setOnClickListener(v -> saveProduct());
    }

    private void saveProduct() {
        String name = getText(etName);
        String priceStr = getText(etPrice);
        String stockStr = getText(etStock);
        String imageUrl = getText(etImageUrl);
        String category = getText(etCategory);

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr) || TextUtils.isEmpty(stockStr)) {
            Toast.makeText(this, R.string.error_empty_fields, Toast.LENGTH_SHORT).show();
            return;
        }

        double price;
        int stock;
        try {
            price = Double.parseDouble(priceStr);
            stock = Integer.parseInt(stockStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Precio o stock inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        setFormEnabled(false);

        Product product = new Product(name, price, stock, imageUrl, category);

        ProductRepository.OnCompleteListener callback = new ProductRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEditProductActivity.this, R.string.success_save, Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                setFormEnabled(true);
                Toast.makeText(AddEditProductActivity.this, R.string.error_save, Toast.LENGTH_LONG).show();
            }
        };

        if (editingProductId != null) {
            product.setId(editingProductId);
            repository.updateProduct(product, callback);
        } else {
            repository.addProduct(product, callback);
        }
    }

    private void setFormEnabled(boolean enabled) {
        etName.setEnabled(enabled);
        etPrice.setEnabled(enabled);
        etStock.setEnabled(enabled);
        etImageUrl.setEnabled(enabled);
        etCategory.setEnabled(enabled);
        findViewById(R.id.btnSave).setEnabled(enabled);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
