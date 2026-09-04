package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
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

    // Long, no BigDecimal: BigDecimal fue un efecto de como JPA Buddy leyo el tipo de
    // columna al hacer ingenieria inversa, no una decision de diseno -- todo el resto del
    // proyecto (Repository<X, Long>, DTOs, etc.) espera Long. SEQUENCE, no IDENTITY: ver
    // Documentation/bitacora-fixes.md incidente #15 (el id se reserva ANTES del INSERT, no
    // hace falta flush() manual para que insert() lo devuelva poblado).
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_seq")
    @SequenceGenerator(name = "producto_seq", sequenceName = "PRODUCTO_SEQ", allocationSize = 1)
    private Long id;

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

    public ProductoEty() {
    }

    public ProductoEty(String nombre, String sku, BigDecimal precio, Integer stock) {
        this.nombre = nombre;
        this.sku = sku;
        this.precio = precio;
        this.stock = stock;
    }
}
