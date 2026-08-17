package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "PRODUCTO")
@Getter
@Setter
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String sku;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    private int stock;

    public Producto() {
    }

    public Producto(String nombre, String sku, BigDecimal precio, int stock) {
        this.nombre = nombre;
        this.sku = sku;
        this.precio = precio;
        this.stock = stock;
    }
}
