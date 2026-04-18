package com.martinalejandroarenasescalera.appdaniel.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.martinalejandroarenasescalera.appdaniel.model.Product;
import com.martinalejandroarenasescalera.appdaniel.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * ProductViewModel – Capa entre la UI y el repositorio (patrón MVVM).
 *
 * Las Activities observan los LiveData expuestos aquí.
 * El ViewModel sobrevive a los cambios de configuración (ej: rotar pantalla).
 */
public class ProductViewModel extends ViewModel {

    private final ProductRepository repository = ProductRepository.getInstance();

    // LiveData filtrado por búsqueda (para el catálogo del cliente)
    private final MutableLiveData<List<Product>> filteredProducts = new MutableLiveData<>();
    private String currentQuery = "";

    // ── LiveData expuestos a la UI ───────────────────────────────

    /** Lista completa de productos (actualizada en tiempo real por Firebase). */
    public LiveData<List<Product>> getProducts() {
        return repository.getProducts();
    }

    /** Lista de productos filtrada por la búsqueda del cliente. */
    public LiveData<List<Product>> getFilteredProducts() {
        return filteredProducts;
    }

    /** Mensajes de error de Firebase. */
    public LiveData<String> getError() {
        return repository.getError();
    }

    // ── Operaciones de negocio ───────────────────────────────────

    public void addProduct(Product product, ProductRepository.OnCompleteListener cb) {
        repository.addProduct(product, cb);
    }

    public void updateProduct(Product product, ProductRepository.OnCompleteListener cb) {
        repository.updateProduct(product, cb);
    }

    public void deleteProduct(String productId, ProductRepository.OnCompleteListener cb) {
        repository.deleteProduct(productId, cb);
    }

    /**
     * Filtra los productos según el texto de búsqueda.
     * Se aplica sobre nombre y categoría del producto.
     */
    public void filterProducts(String query) {
        currentQuery = query.toLowerCase().trim();
        List<Product> allProducts = repository.getProducts().getValue();
        if (allProducts == null) {
            filteredProducts.setValue(new ArrayList<>());
            return;
        }
        if (currentQuery.isEmpty()) {
            filteredProducts.setValue(allProducts);
            return;
        }
        List<Product> result = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.getName().toLowerCase().contains(currentQuery)
                    || (p.getCategory() != null && p.getCategory().toLowerCase().contains(currentQuery))) {
                result.add(p);
            }
        }
        filteredProducts.setValue(result);
    }

    /** Inicializa la lista filtrada cuando se cargan los productos por primera vez. */
    public void onProductsLoaded(List<Product> products) {
        if (currentQuery.isEmpty()) {
            filteredProducts.setValue(products);
        } else {
            filterProducts(currentQuery);
        }
    }
}
