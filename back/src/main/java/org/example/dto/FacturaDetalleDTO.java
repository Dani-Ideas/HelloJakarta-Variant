package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record FacturaDetalleDTO(
        Long id,

        @NotNull(message = "El id del producto es obligatorio")
        Long productoId,

        String nombreProducto,

        @Positive(message = "La cantidad debe ser mayor a 0")
        int cantidad,

        BigDecimal precioUnitario,
        BigDecimal subtotal
) {
}
