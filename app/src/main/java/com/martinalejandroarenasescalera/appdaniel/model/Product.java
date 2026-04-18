package com.martinalejandroarenasescalera.appdaniel.model;

/**
 * Modelo de Producto – se mapea directamente con Firebase Realtime Database.
 * La ruta en Firebase es: /products/{id}
 *
 * IMPORTANTE: El constructor vacío es OBLIGATORIO para que Firebase
 * pueda deserializar los datos automáticamente.
 */
public class Product {

    private String id;        // Clave generada por Firebase (push key)
    private String name;      // Nombre del producto
    private double price;     // Precio en MXN
    private int stock;        // Unidades disponibles
    private String imageUrl;  // URL de imagen (Firebase Storage o URL externa)
    private String category;  // Categoría (frutas, verduras, ropa, etc.)
    private long createdAt;   // Timestamp de creación (epoch ms)

    // ── Constructor vacío requerido por Firebase ────────────────
    public Product() {}

    // ── Constructor completo ─────────────────────────────────────
    public Product(String name, double price, int stock, String imageUrl, String category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.category = category;
        this.createdAt = System.currentTimeMillis();
    }

    // ── Getters y Setters ────────────────────────────────────────
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean hasStock() { return stock > 0; }
}
