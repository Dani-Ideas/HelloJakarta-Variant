package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for {@link org.example.model.FacturaEty}
 */
public record FacturaDto(BigDecimal id, @Size(max = 255) String cliente, LocalDate fecha,
                         @NotNull @Size(max = 255) String numero, BigDecimal total,
                         SesionCajaDto sesionCaja) implements Serializable {
}