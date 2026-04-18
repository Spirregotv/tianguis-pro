package com.martinalejandroarenasescalera.appdaniel.model;

/**
 * Registro de una venta completada.
 * Se guarda en Firebase bajo la ruta: /sales/{userId}/{id}
 *
 * Constructor vacío requerido por Firebase.
 */
public class SaleRecord {

    private String id;
    private long timestamp;      // Época en milisegundos
    private double total;        // Monto total de la venta
    private int itemCount;       // Número de productos comprados
    private String paymentId;    // ID de pago de Mercado Pago
    private String status;       // "APPROVED" | "PENDING" | "REJECTED"

    public SaleRecord() {}

    public SaleRecord(double total, int itemCount, String paymentId, String status) {
        this.timestamp = System.currentTimeMillis();
        this.total = total;
        this.itemCount = itemCount;
        this.paymentId = paymentId;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public int getItemCount() { return itemCount; }
    public void setItemCount(int itemCount) { this.itemCount = itemCount; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
