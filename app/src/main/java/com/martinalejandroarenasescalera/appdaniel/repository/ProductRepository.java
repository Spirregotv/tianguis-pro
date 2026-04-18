package com.martinalejandroarenasescalera.appdaniel.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.martinalejandroarenasescalera.appdaniel.model.Product;
import com.martinalejandroarenasescalera.appdaniel.model.SaleRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * ProductRepository – Capa de acceso a datos (Firebase Realtime Database).
 *
 * Rutas en Firebase:
 *   /products/{pushKey}  → inventario de productos
 *   /sales/{userId}/{pushKey} → registros de ventas del vendedor
 *
 * NOTA: Firebase Realtime Database debe estar habilitado en tu proyecto.
 *   Reglas sugeridas para desarrollo (cámbialas antes de publicar):
 *   {
 *     "rules": {
 *       "products": { ".read": true, ".write": "auth != null" },
 *       "sales":    { "$uid": { ".read": "$uid === auth.uid", ".write": "$uid === auth.uid" } }
 *     }
 *   }
 */
public class ProductRepository {

    private static ProductRepository instance;

    private final DatabaseReference productsRef;
    private final DatabaseReference salesRef;

    private final MutableLiveData<List<Product>> productsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<SaleRecord>> salesLiveData = new MutableLiveData<>();

    // ── Singleton ────────────────────────────────────────────────
    public static synchronized ProductRepository getInstance() {
        if (instance == null) {
            instance = new ProductRepository();
        }
        return instance;
    }

    private ProductRepository() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        productsRef = db.getReference("products");
        salesRef = db.getReference("sales");
        observeProducts();
    }

    // ── Observación en tiempo real de productos ──────────────────
    private void observeProducts() {
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Product> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Product p = child.getValue(Product.class);
                    if (p != null) {
                        p.setId(child.getKey());
                        list.add(p);
                    }
                }
                productsLiveData.postValue(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                errorLiveData.postValue(error.getMessage());
            }
        });
    }

    // ── Getters de LiveData ──────────────────────────────────────
    public LiveData<List<Product>> getProducts() { return productsLiveData; }
    public LiveData<String> getError() { return errorLiveData; }
    public LiveData<List<SaleRecord>> getSales() { return salesLiveData; }

    // ── CRUD de Productos ────────────────────────────────────────

    public void addProduct(Product product, OnCompleteListener callback) {
        String key = productsRef.push().getKey();
        if (key == null) {
            callback.onFailure("No se pudo generar ID");
            return;
        }
        product.setId(key);
        productsRef.child(key).setValue(product)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void updateProduct(Product product, OnCompleteListener callback) {
        productsRef.child(product.getId()).setValue(product)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void deleteProduct(String productId, OnCompleteListener callback) {
        productsRef.child(productId).removeValue()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Reduce el stock de un producto en 'quantity' unidades tras una venta. */
    public void decrementStock(String productId, int quantity) {
        productsRef.child(productId).child("stock")
                .get()
                .addOnSuccessListener(snapshot -> {
                    Integer currentStock = snapshot.getValue(Integer.class);
                    if (currentStock != null && currentStock >= quantity) {
                        productsRef.child(productId).child("stock")
                                .setValue(currentStock - quantity);
                    }
                });
    }

    // ── Registro de ventas ───────────────────────────────────────

    public void saveSale(String userId, SaleRecord sale, OnCompleteListener callback) {
        String key = salesRef.child(userId).push().getKey();
        if (key == null) {
            callback.onFailure("No se pudo guardar la venta");
            return;
        }
        sale.setId(key);
        salesRef.child(userId).child(key).setValue(sale)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /** Carga las ventas del día actual para el dashboard. */
    public void loadTodaySales(String userId, OnSalesLoadedListener listener) {
        long startOfDay = getStartOfDayTimestamp();
        salesRef.child(userId)
                .orderByChild("timestamp")
                .startAt(startOfDay)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<SaleRecord> list = new ArrayList<>();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            SaleRecord s = child.getValue(SaleRecord.class);
                            if (s != null) {
                                s.setId(child.getKey());
                                list.add(s);
                            }
                        }
                        listener.onLoaded(list);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        listener.onError(error.getMessage());
                    }
                });
    }

    private long getStartOfDayTimestamp() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    // ── Interfaces de callback ───────────────────────────────────

    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnSalesLoadedListener {
        void onLoaded(List<SaleRecord> sales);
        void onError(String errorMessage);
    }
}
