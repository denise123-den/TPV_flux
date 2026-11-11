package com.example.tpv_flux.model;

/**
 * Modelo de producto para el catálogo de Ventas y la tabla de Productos.
 */
public class ProductItem {

    private final Integer id;
    private final String name;
    private final String sku;
    private final String category;
    private final String shortTag;
    private int stock;
    private final double price;
    private String photo;

    public ProductItem(String name, String sku, String category, String shortTag, int stock, double price) {
        this(null, name, sku, category, shortTag, stock, price, null);
    }

    public ProductItem(String name, String sku, String category, String shortTag, int stock, double price, String photo) {
        this(null, name, sku, category, shortTag, stock, price, photo);
    }

    public ProductItem(Integer id, String name, String sku, String category, String shortTag, int stock, double price, String photo) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.shortTag = shortTag;
        this.stock = stock;
        this.price = price;
        this.photo = photo;
    }

    public Integer getId() {
        return id;
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

    public void setStock(int stock) {
        this.stock = stock;
    }

    public double getPrice() {
        return price;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
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
