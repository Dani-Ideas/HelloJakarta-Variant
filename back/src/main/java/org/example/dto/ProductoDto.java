package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link org.example.model.ProductoEty}
 */
public record ProductoDto(
        Long id,
        @NotNull @Size(max = 255) String nombre,
        @NotNull @Positive BigDecimal precio,
        @Size(max = 255) String sku,
        @PositiveOrZero Integer stock
) implements Serializable {
}
