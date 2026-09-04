package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "PRODUCTO")
public class ProductoEty {
    @Id
    @Column(name = "ID", nullable = false, precision = 19)
    private BigDecimal id;

    @Size(max = 255)
    @NotNull
    @Column(name = "NOMBRE", nullable = false)
    private String nombre;

    @NotNull
    @Column(name = "PRECIO", nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Size(max = 255)
    @Column(name = "SKU")
    private String sku;

    @Column(name = "STOCK")
    private Integer stock;


}