package org.example.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "FACTURA_DETALLE")
@Getter
@Setter
public class FacturaDetalle {

    // SEQUENCE, no IDENTITY: ver Producto.java para el detalle completo del porque.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "factura_detalle_seq")
    @SequenceGenerator(name = "factura_detalle_seq", sequenceName = "FACTURA_DETALLE_SEQ", allocationSize = 1)
    private Long id;

    // Sin esta anotacion, Factura -> detalles -> factura -> detalles... genera un ciclo infinito al serializar a JSON
    @ManyToOne
    @JoinColumn(name = "factura_id")
    @JsonbTransient
    private Factura factura;

    @ManyToOne
    @JoinColumn(name = "producto_id")
    private Producto producto;

    private int cantidad;

    @Column(precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(precision = 10, scale = 2)
    private BigDecimal subtotal;

    public FacturaDetalle() {
    }
}
