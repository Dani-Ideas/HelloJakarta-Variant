package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for {@link org.example.model.SesionCajaEty}
 */
public record SesionCajaDto(BigDecimal id, @NotNull @Size(max = 255) String cajero, Boolean cerrada, Instant fapertura,
                            Instant fcierre, @NotNull @Size(max = 255) String locacion,
                            @NotNull BigDecimal montoapertura, BigDecimal montocierre) implements Serializable {
}