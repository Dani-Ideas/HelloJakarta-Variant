package org.example.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SesionCajaDTO {

    private Long id;
    private LocalDateTime fApertura;
    private LocalDateTime fCierre;
    private Boolean cerrada;
    private BigDecimal montoApertura;
    private BigDecimal montoCierre;
    private String cajero = "cajero1";
    private String locacion = "tienda1";

    // Sin @NotEmpty a proposito: al abrir una caja nueva, esta lista empieza vacia.
    @Valid
    private List<FacturaDTO> facturas;
}
