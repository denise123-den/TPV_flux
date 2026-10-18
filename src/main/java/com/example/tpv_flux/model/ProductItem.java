package com.example.tpv_flux.model;

/**
 * Modelo de producto para el catálogo de Ventas y la tabla de Productos.
 */
public class ProductItem {

    private final String name;
    private final String sku;
    private final String category;
    private final String shortTag;
    private final int stock;
    private final double price;

    public ProductItem(String name, String sku, String category, String shortTag, int stock, double price) {
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.shortTag = shortTag;
        this.stock = stock;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public String getSku() {
        return sku;
    }

    public String getCategory() {
        return category;
    }

    public String getShortTag() {
        return shortTag;
    }

    public int getStock() {
        return stock;
    }

    public double getPrice() {
        return price;
    }

    public String getStatus() {
        if (stock <= 0) {
            return "Sin stock";
        } else if (stock <= 6) {
            return "Stock bajo";
        }
        return "Disponible";
    }
}
