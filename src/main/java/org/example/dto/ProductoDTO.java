package org.example.dto;

import java.math.BigDecimal;

public class ProductoDTO {

    private Long id;
    private String nombre;
    private String sku;
    private BigDecimal precio;
    private int stock;

    public ProductoDTO() {
    }

    public ProductoDTO(Long id, String nombre, String sku, BigDecimal precio, int stock) {
        this.id = id;
        this.nombre = nombre;
        this.sku = sku;
        this.precio = precio;
        this.stock = stock;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}
