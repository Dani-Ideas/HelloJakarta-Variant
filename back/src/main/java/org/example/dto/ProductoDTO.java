package org.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

// record, no class: es un contrato de datos inmutable -- se construye completo de una vez
// (nunca con setters), y Java genera solo por tener el record los accessors (nombre(),
// no getNombre()), constructor, equals/hashCode y toString. Ya no hace falta Lombok aqui.
// Las anotaciones de validacion van sobre cada componente del record.
public record ProductoDTO(
        Long id,

        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        @NotBlank(message = "El SKU es obligatorio")
        String sku,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a 0")
        BigDecimal precio,

        @PositiveOrZero(message = "El stock no puede ser negativo")
        int stock
) {
}
