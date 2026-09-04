package org.example.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.math.BigDecimal;

// record: cada componente es opcional -- null significa "no tocar este campo". Se habia
// perdido al regenerar model/dto/mapper, se recrea igual que la version original.
public record ProductoPatchDto(
        String nombre,
        @Positive BigDecimal precio,
        String sku,
        @PositiveOrZero Integer stock
) implements Serializable {
}
