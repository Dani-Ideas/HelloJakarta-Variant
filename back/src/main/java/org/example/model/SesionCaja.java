package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "SESION_CAJA")
@Getter
@Setter
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate fecha;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal apertura = BigDecimal.ZERO;

    // Sin "nullable = false": mientras la caja sigue abierta, este campo NO tiene valor
    // todavia. Se llena hasta el UPDATE de cierre.
    @Column(precision = 10, scale = 2)
    private BigDecimal cierre;

    // boolean en vez de enum (decision tuya) -- Lombok genera isCerrada(), no getCerrada().
    private boolean cerrada;

    @Column(nullable = false)
    private String cajero = "cajero1";

    @Column(nullable = false)
    private String locacion = "tienda1";

    // El lado "uno" de la relacion: no crea ninguna columna, solo refleja el @ManyToOne
    // que vive del lado de Factura. mappedBy = "sesionCaja" tiene que decir EXACTAMENTE
    // el nombre del campo que declaramos en Factura.java.
    @OneToMany(mappedBy = "sesionCaja")
    private List<Factura> facturas = new ArrayList<>();

    public SesionCaja() {
    }
}
