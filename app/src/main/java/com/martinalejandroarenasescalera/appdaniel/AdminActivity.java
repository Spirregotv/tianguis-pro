package com.martinalejandroarenasescalera.appdaniel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.martinalejandroarenasescalera.appdaniel.adapter.ProductAdapter;
import com.martinalejandroarenasescalera.appdaniel.model.Product;
import com.martinalejandroarenasescalera.appdaniel.viewmodel.ProductViewModel;

/**
 * AdminActivity – Gestión del inventario (CRUD de productos).
 *
 * Funcionalidades:
 *  • Lista de productos en tiempo real (Firebase Realtime Database)
 *  • FAB para agregar un nuevo producto
 *  • Botón Editar → abre AddEditProductActivity con los datos del producto
 *  • Botón Eliminar → muestra diálogo de confirmación y borra de Firebase
 *  • SwipeRefreshLayout para actualización manual
 */
public class AdminActivity extends AppCompatActivity implements ProductAdapter.OnProductActionListener {

    public static final String EXTRA_PRODUCT_ID = "product_id";
    public static final String EXTRA_PRODUCT_NAME = "product_name";
    public static final String EXTRA_PRODUCT_PRICE = "product_price";
    public static final String EXTRA_PRODUCT_STOCK = "product_stock";
    public static final String EXTRA_PRODUCT_IMAGE = "product_image";
    public static final String EXTRA_PRODUCT_CATEGORY = "product_category";

    private ProductViewModel viewModel;
    private ProductAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.admin_title);
        }

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.recyclerProducts);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        tvEmpty = findViewById(R.id.tvEmpty);
        ExtendedFloatingActionButton fab = findViewById(R.id.fabAddProduct);

        adapter = new ProductAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Observar cambios en Firebase en tiempo real
        viewModel.getProducts().observe(this, products -> {
            swipeRefresh.setRefreshing(false);
            adapter.submitList(products);
            tvEmpty.setVisibility(products == null || products.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Mostrar errores de Firebase
        viewModel.getError().observe(this, error -> {
            if (error != null) Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        });

        swipeRefresh.setColorSchemeResources(R.color.md_theme_primary);
        swipeRefresh.setOnRefreshListener(() -> swipeRefresh.setRefreshing(false));

        fab.setOnClickListener(v ->
                startActivity(new Intent(this, AddEditProductActivity.class)));
    }

    // ── Acciones del adapter ─────────────────────────────────────

    @Override
    public void onEdit(Product product) {
        Intent intent = new Intent(this, AddEditProductActivity.class);
        intent.putExtra(EXTRA_PRODUCT_ID, product.getId());
        intent.putExtra(EXTRA_PRODUCT_NAME, product.getName());
        intent.putExtra(EXTRA_PRODUCT_PRICE, product.getPrice());
        intent.putExtra(EXTRA_PRODUCT_STOCK, product.getStock());
        intent.putExtra(EXTRA_PRODUCT_IMAGE, product.getImageUrl());
        intent.putExtra(EXTRA_PRODUCT_CATEGORY, product.getCategory());
        startActivity(intent);
    }

    @Override
    public void onDelete(Product product) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(getString(R.string.confirm_delete, product.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) ->
                        viewModel.deleteProduct(product.getId(), new com.martinalejandroarenasescalera.appdaniel.repository.ProductRepository.OnCompleteListener() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(AdminActivity.this, "Producto eliminado", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(AdminActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                            }
                        }))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
