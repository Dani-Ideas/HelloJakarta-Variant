package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "SESION_CAJA")
public class SesionCajaEty {

    // IDENTITY, no SEQUENCE: SesionCaja/Usuario se dejaron con la estrategia original a
    // proposito, fuera del alcance de la migracion de Producto/Factura -- ver
    // Documentation/bitacora-fixes.md incidente #15.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "CAJERO", nullable = false)
    private String cajero;

    private boolean cerrada;

    // fApertura/fCierre, no fapertura/fcierre: camelCase, consistente con el resto del
    // proyecto. LocalDateTime (fecha + hora), no LocalDate -- para poder calcular duracion
    // real de la sesion.
    @Column(name = "FAPERTURA")
    private LocalDateTime fApertura;

    @Column(name = "FCIERRE")
    private LocalDateTime fCierre;

    @Size(max = 255)
    @NotNull
    @Column(name = "LOCACION", nullable = false)
    private String locacion;

    @NotNull
    @Column(name = "MONTOAPERTURA", nullable = false, precision = 10, scale = 2)
    private BigDecimal montoApertura;

    @Column(name = "MONTOCIERRE", precision = 10, scale = 2)
    private BigDecimal montoCierre;

    // Lado inverso de la relacion con Factura -- se habia perdido en la regeneracion.
    @OneToMany(mappedBy = "sesionCaja")
    private List<FacturaEty> facturas = new ArrayList<>();

    public SesionCajaEty() {
    }
}
