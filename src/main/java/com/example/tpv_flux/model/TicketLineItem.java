package com.example.tpv_flux.model;

/**
 * Representa un renglón dentro del panel "Venta actual" (Ticket).
 */
public class TicketLineItem {

    private final ProductItem product;
    private int quantity;

    public TicketLineItem(ProductItem product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public ProductItem getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getSubtotal() {
        return product.getPrice() * quantity;
    }
}
