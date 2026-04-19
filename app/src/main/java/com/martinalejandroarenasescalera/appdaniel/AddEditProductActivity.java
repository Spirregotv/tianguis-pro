package com.martinalejandroarenasescalera.appdaniel;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.martinalejandroarenasescalera.appdaniel.model.Product;
import com.martinalejandroarenasescalera.appdaniel.repository.ProductRepository;

import java.util.UUID;

/**
 * AddEditProductActivity – Formulario para crear o editar un producto.
 *
 * Flujo de imagen:
 *  1. Usuario toca "Seleccionar imagen de galería"
 *  2. Se abre el selector de imágenes del sistema
 *  3. Al elegir, se muestra la vista previa con Glide
 *  4. Al guardar: si hay imagen nueva → se sube a Firebase Storage
 *     → se obtiene la URL de descarga → se guarda en el producto
 */
public class AddEditProductActivity extends AppCompatActivity {

    private TextInputEditText etName, etPrice, etStock, etImageUrl, etCategory;
    private ImageView imgPreview;
    private ProgressBar progressBar;
    private ProductRepository repository;

    /** URI de la imagen elegida de la galería (null si no se eligió ninguna). */
    private Uri selectedImageUri = null;

    private String editingProductId = null;

    // Lanzador del selector de imágenes del sistema
    private final ActivityResultLauncher<String> imagePicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).centerCrop().into(imgPreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        repository = ProductRepository.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etName      = findViewById(R.id.etProductName);
        etPrice     = findViewById(R.id.etProductPrice);
        etStock     = findViewById(R.id.etProductStock);
        etImageUrl  = findViewById(R.id.etProductImageUrl);
        etCategory  = findViewById(R.id.etProductCategory);
        imgPreview  = findViewById(R.id.imgPreview);
        progressBar = findViewById(R.id.progressBar);
        MaterialButton btnSave      = findViewById(R.id.btnSave);
        MaterialButton btnPickImage = findViewById(R.id.btnPickImage);

        btnPickImage.setOnClickListener(v -> imagePicker.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProduct());

        // ── Cargar datos si estamos editando ────────────────────
        editingProductId = getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_ID);
        if (editingProductId != null) {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(R.string.edit_product_title);

            etName.setText(getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_NAME));
            etPrice.setText(String.valueOf(getIntent().getDoubleExtra(AdminActivity.EXTRA_PRODUCT_PRICE, 0)));
            etStock.setText(String.valueOf(getIntent().getIntExtra(AdminActivity.EXTRA_PRODUCT_STOCK, 0)));
            etCategory.setText(getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_CATEGORY));

            String existingImage = getIntent().getStringExtra(AdminActivity.EXTRA_PRODUCT_IMAGE);
            if (existingImage != null) {
                etImageUrl.setText(existingImage);
                Glide.with(this).load(existingImage).centerCrop().into(imgPreview);
            }
        } else {
            if (getSupportActionBar() != null)
                getSupportActionBar().setTitle(R.string.add_product_title);
        }
    }

    private void saveProduct() {
        String name     = getText(etName);
        String priceStr = getText(etPrice);
        String stockStr = getText(etStock);
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

        if (selectedImageUri != null) {
            // Subir imagen nueva a Firebase Storage
            uploadImageAndSave(name, price, stock, category);
        } else {
            // Usar la URL manual o la existente
            String imageUrl = getText(etImageUrl);
            persistProduct(name, price, stock, imageUrl, category);
        }
    }

    /**
     * Sube la imagen seleccionada a Firebase Storage y luego guarda el producto.
     */
    private void uploadImageAndSave(String name, double price, int stock, String category) {
        String fileName = "products/" + UUID.randomUUID() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(fileName);

        ref.putFile(selectedImageUri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null)
                        throw task.getException();
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri ->
                        persistProduct(name, price, stock, downloadUri.toString(), category))
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    setFormEnabled(true);
                    Toast.makeText(this,
                            "Error al subir imagen: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Guarda o actualiza el producto en Firebase Realtime Database.
     */
    private void persistProduct(String name, double price, int stock, String imageUrl, String category) {
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
        findViewById(R.id.btnPickImage).setEnabled(enabled);
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
