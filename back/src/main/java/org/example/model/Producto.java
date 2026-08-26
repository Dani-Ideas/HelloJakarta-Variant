package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "PRODUCTO")
@Getter
@Setter
public class Producto {

    // SEQUENCE, no IDENTITY: con SEQUENCE el id se reserva ANTES del INSERT (via nextval
    // aparte), asi que el proveedor lo conoce de inmediato sin tener que forzar el INSERT
    // real con un flush(). Con IDENTITY, el id no se conoce hasta que el INSERT se ejecuta
    // de verdad contra la base -- por eso antes hacia falta EntityManager+flush() en
    // ProductoServiceImpl.crear() (ver Documentation/bitacora-fixes.md incidente #15).
    // allocationSize = 1 a proposito: sin esto, el default de JPA es 50 (el proveedor
    // reserva de a 50 ids en memoria por cada llamada real a nextval) -- util en produccion
    // real, pero confuso para aprender (los ids visibles saltarian de 50 en 50 sin razon
    // aparente la primera vez que se reinicia el server).
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "producto_seq")
    @SequenceGenerator(name = "producto_seq", sequenceName = "PRODUCTO_SEQ", allocationSize = 1)
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
