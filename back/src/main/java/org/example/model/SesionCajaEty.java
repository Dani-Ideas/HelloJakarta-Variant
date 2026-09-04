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
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "SESION_CAJA")
public class SesionCajaEty {
    @Id
    @Column(name = "ID", nullable = false, precision = 19)
    private BigDecimal id;

    @Size(max = 255)
    @NotNull
    @Column(name = "CAJERO", nullable = false)
    private String cajero;

    @Column(name = "CERRADA")
    private Boolean cerrada;

    @Column(name = "FAPERTURA")
    private Instant fapertura;

    @Column(name = "FCIERRE")
    private Instant fcierre;

    @Size(max = 255)
    @NotNull
    @Column(name = "LOCACION", nullable = false)
    private String locacion;

    @NotNull
    @Column(name = "MONTOAPERTURA", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoapertura;

    @Column(name = "MONTOCIERRE", precision = 10, scale = 2)
    private BigDecimal montocierre;


}