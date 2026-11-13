package com.example.tpv_flux.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Locale;

@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(name = "costo", nullable = false, precision = 10, scale = 2)
    private BigDecimal costo = BigDecimal.ZERO;

    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta = BigDecimal.ZERO;

    /**
     * Nombre del archivo de imagen (ej. "funda.png"), ruta en resources o ruta en disco.
     */
    @Column(name = "foto", columnDefinition = "TEXT")
    private String foto;

    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual = 0;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    public Producto() {
    }

    public Producto(String nombre, String descripcion, Categoria categoria, BigDecimal costo, BigDecimal precioVenta, String foto, Integer stockActual, Boolean activo) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.costo = costo;
        this.precioVenta = precioVenta;
        this.foto = foto;
        this.stockActual = stockActual;
        this.activo = activo;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(Integer stockActual) {
        this.stockActual = stockActual;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    /**
     * Extrae el SKU desde la descripción si tiene el formato "SKU: XXX | ..." o genera uno a partir del ID.
     */
    public String extractSku() {
        if (descripcion != null) {
            for (String part : descripcion.split("\\|")) {
                String trimmed = part.trim();
                if (trimmed.toUpperCase(Locale.ROOT).startsWith("SKU:")) {
                    return trimmed.substring(4).trim();
                }
            }
        }
        return String.format("PRD-%03d", id != null ? id : 0);
    }

    /**
     * Extrae la etiqueta corta desde la descripción si tiene el formato "TAG: XXX" o usa la primera palabra del nombre.
     */
    public String extractShortTag() {
        if (descripcion != null) {
            for (String part : descripcion.split("\\|")) {
                String trimmed = part.trim();
                if (trimmed.toUpperCase(Locale.ROOT).startsWith("TAG:")) {
                    return trimmed.substring(4).trim().toUpperCase(Locale.ROOT);
                }
            }
        }
        if (nombre != null && !nombre.isBlank()) {
            String firstWord = nombre.trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
            return firstWord.length() > 8 ? firstWord.substring(0, 8) : firstWord;
        }
        return "ITEM";
    }

    public ProductItem toProductItem() {
        String shortTag = extractShortTag();
        String resolvedFoto = com.example.tpv_flux.util.ImageUtil.resolvePhotoReference(foto, nombre, shortTag);
        return new ProductItem(
                id,
                nombre,
                extractSku(),
                categoria != null ? categoria.getNombre() : "General",
                shortTag,
                stockActual != null ? stockActual : 0,
                precioVenta != null ? precioVenta.doubleValue() : 0.0,
                resolvedFoto
        );
    }
}
