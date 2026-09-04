package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link org.example.model.FacturaDetalleEty}
 */
public record FacturaDetalleDto(
        Long id,
        @NotNull @Positive Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        // Sin "factura" aqui a proposito -- si lo tuviera, seria un ciclo (Factura ->
        // detalles -> FacturaDetalle -> factura -> Factura -> ...). El detalle siempre se
        // accede DESDE su Factura, nunca al reves.
        @NotNull ProductoDto producto
) implements Serializable {
}
