package com.martinalejandroarenasescalera.appdaniel;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.martinalejandroarenasescalera.appdaniel.adapter.ClientProductAdapter;
import com.martinalejandroarenasescalera.appdaniel.model.CartItem;
import com.martinalejandroarenasescalera.appdaniel.model.Product;
import com.martinalejandroarenasescalera.appdaniel.viewmodel.ProductViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * ClientActivity – Catálogo visual de productos para el cliente.
 *
 * • Grid de 2 columnas con tarjetas de producto
 * • Barra de búsqueda en tiempo real
 * • Botón flotante de carrito con contador de ítems
 * • El carrito se pasa como Serializable al CartActivity
 */
public class ClientActivity extends AppCompatActivity implements ClientProductAdapter.OnAddToCartListener {

    // Carrito compartido durante la sesión del cliente
    public static final List<CartItem> cart = new ArrayList<>();

    private ProductViewModel viewModel;
    private ClientProductAdapter adapter;
    private FloatingActionButton fabCart;
    private TextView tvCartCount, tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        // Limpiar carrito al abrir el catálogo
        cart.clear();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.client_title);
        }

        viewModel = new ViewModelProvider(this).get(ProductViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.recyclerProducts);
        fabCart = findViewById(R.id.fabCart);
        tvCartCount = findViewById(R.id.tvCartCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        TextInputEditText etSearch = findViewById(R.id.etSearch);

        adapter = new ClientProductAdapter(this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        // Observar productos (lista completa) y pasar al viewModel para filtrado
        viewModel.getProducts().observe(this, products -> {
            viewModel.onProductsLoaded(products);
            tvEmpty.setVisibility(products == null || products.isEmpty() ? View.VISIBLE : View.GONE);
        });

        // Observar lista filtrada
        viewModel.getFilteredProducts().observe(this, products -> {
            adapter.submitList(products);
        });

        // Búsqueda en tiempo real
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.filterProducts(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        fabCart.setOnClickListener(v -> {
            if (cart.isEmpty()) {
                Toast.makeText(this, R.string.cart_empty, Toast.LENGTH_SHORT).show();
            } else {
                startActivity(new Intent(this, CartActivity.class));
            }
        });

        updateCartBadge();
    }

    @Override
    public void onAddToCart(Product product) {
        // Verificar si el producto ya está en el carrito
        for (CartItem item : cart) {
            if (item.getProduct().getId().equals(product.getId())) {
                if (item.increment()) {
                    updateCartBadge();
                    Toast.makeText(this, "+" + product.getName(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No hay más stock disponible", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
        // Producto nuevo en el carrito
        cart.add(new CartItem(product, 1));
        updateCartBadge();
        Toast.makeText(this, product.getName() + " agregado al carrito", Toast.LENGTH_SHORT).show();
    }

    private void updateCartBadge() {
        int total = 0;
        for (CartItem item : cart) total += item.getQuantity();
        if (total > 0) {
            tvCartCount.setVisibility(View.VISIBLE);
            tvCartCount.setText(String.valueOf(total));
        } else {
            tvCartCount.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartBadge();
    }
}
