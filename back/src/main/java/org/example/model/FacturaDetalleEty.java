package org.example.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "FACTURA_DETALLE")
public class FacturaDetalleEty {
    @Id
    @Column(name = "ID", nullable = false, precision = 19)
    private BigDecimal id;

    @Column(name = "CANTIDAD")
    private Integer cantidad;

    @Column(name = "PRECIOUNITARIO", precision = 10, scale = 2)
    private BigDecimal preciounitario;

    @Column(name = "SUBTOTAL", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    private FacturaEty factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private ProductoEty producto;


}