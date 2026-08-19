package org.example.model;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "FACTURA")
@Getter
@Setter
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numero;

    private LocalDate fecha;

    private String cliente;

    @Column(precision = 10, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaDetalle> detalles = new ArrayList<>();

    // El lado "muchos": este SI crea la columna real (sesion_caja_id) en la tabla FACTURA.
    // Una Factura pertenece a UNA SesionCaja (no una lista) -- por eso @ManyToOne, no
    // @OneToMany, y el tipo es SesionCaja, no List<SesionCaja>.
    @ManyToOne
    @JoinColumn(name = "sesion_caja_id")
    // Sin esto, serializar una Factura -> sesionCaja -> facturas -> sesionCaja -> ...
    // entra en un ciclo infinito, igual que paso antes con FacturaDetalle -> Factura.
    @JsonbTransient
    private SesionCaja sesionCaja;

    public Factura() {
    }
}
