package org.example.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// record: cada componente es opcional (tipos wrapper, nunca primitivos) -- null significa
// "no tocar este campo". Con "int stock" no habria forma de distinguir "el cliente no
// mando stock" de "el cliente mando stock=0"; con "Integer stock" si.
public record ProductoPatchDTO(
        String nombre,
        String sku,

        @Positive(message = "El precio debe ser mayor a 0")
        BigDecimal precio,

        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock
) {
}
