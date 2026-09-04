package org.example.dto;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link org.example.model.FacturaDetalleEty}
 */
public record FacturaDetalleDto(BigDecimal id, Integer cantidad, BigDecimal preciounitario, BigDecimal subtotal,
                                FacturaDto factura, ProductoDto producto) implements Serializable {
}