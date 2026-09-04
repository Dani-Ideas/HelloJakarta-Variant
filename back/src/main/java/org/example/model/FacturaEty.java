package org.example.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "FACTURA")
public class FacturaEty {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "factura_seq")
    @SequenceGenerator(name = "factura_seq", sequenceName = "FACTURA_SEQ", allocationSize = 1)
    private Long id;

    @Size(max = 255)
    @Column(name = "CLIENTE")
    private String cliente;

    @Column(name = "FECHA")
    private LocalDate fecha;

    @Size(max = 255)
    @NotNull
    @Column(name = "NUMERO", nullable = false, unique = true)
    private String numero;

    @Column(name = "TOTAL", precision = 10, scale = 2)
    private BigDecimal total;

    // Lado inverso de la relacion con FacturaDetalle -- se habia perdido en la
    // regeneracion con JPA Buddy (solo quedo el lado dueno, FacturaDetalleEty.factura).
    // Sin esto, una Factura no puede exponer su lista de lineas en Java.
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaDetalleEty> detalles = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id")
    // Sin esto, serializar Factura -> sesionCaja -> facturas -> sesionCaja -> ... entra en
    // un ciclo infinito.
    @JsonbTransient
    private SesionCajaEty sesionCaja;

    public FacturaEty() {
    }
}
