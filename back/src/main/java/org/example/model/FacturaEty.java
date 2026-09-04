package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "FACTURA")
public class FacturaEty {
    @Id
    @Column(name = "ID", nullable = false, precision = 19)
    private BigDecimal id;

    @Size(max = 255)
    @Column(name = "CLIENTE")
    private String cliente;

    @Column(name = "FECHA")
    private LocalDate fecha;

    @Size(max = 255)
    @NotNull
    @Column(name = "NUMERO", nullable = false)
    private String numero;

    @Column(name = "TOTAL", precision = 10, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_caja_id")
    private SesionCajaEty sesionCaja;


}