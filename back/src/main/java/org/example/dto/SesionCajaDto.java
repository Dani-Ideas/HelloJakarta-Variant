package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for {@link org.example.model.SesionCajaEty}
 */
public record SesionCajaDto(
        Long id,
        @NotNull @Size(max = 255) String cajero,
        Boolean cerrada,
        LocalDateTime fApertura,
        LocalDateTime fCierre,
        @NotNull @Size(max = 255) String locacion,
        @NotNull BigDecimal montoApertura,
        BigDecimal montoCierre
) implements Serializable {
}
