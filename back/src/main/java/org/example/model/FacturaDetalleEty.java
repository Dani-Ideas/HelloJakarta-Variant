package org.example.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

@Getter
@Setter
@Entity
@Table(name = "FACTURA_DETALLE")
public class FacturaDetalleEty {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "factura_detalle_seq")
    @SequenceGenerator(name = "factura_detalle_seq", sequenceName = "FACTURA_DETALLE_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "CANTIDAD")
    private Integer cantidad;

    // precioUnitario, no "preciounitario": camelCase, consistente con el resto del proyecto.
    @Column(name = "PRECIOUNITARIO", precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "SUBTOTAL", precision = 10, scale = 2)
    private BigDecimal subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_id")
    // Lado dueno de la relacion con Factura -- sin esto, Factura -> detalles -> factura ->
    // detalles... entra en un ciclo infinito al serializar a JSON.
    @JsonbTransient
    private FacturaEty factura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private ProductoEty producto;

    public FacturaDetalleEty() {
    }
}
