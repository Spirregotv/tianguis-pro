package com.martinalejandroarenasescalera.appdaniel.model;

/**
 * Representa un ítem dentro del carrito de compras.
 * Combina un Producto con la cantidad seleccionada por el cliente.
 * El carrito se gestiona localmente en memoria (no en Firebase).
 */
public class CartItem {

    private Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    /** Subtotal = precio × cantidad */
    public double getSubtotal() {
        return product.getPrice() * quantity;
    }

    /** Incrementa la cantidad en 1, respetando el stock disponible. */
    public boolean increment() {
        if (quantity < product.getStock()) {
            quantity++;
            return true;
        }
        return false;
    }

    /** Decrementa la cantidad en 1. Retorna false si ya es 1 (mínimo). */
    public boolean decrement() {
        if (quantity > 1) {
            quantity--;
            return true;
        }
        return false;
    }
}
