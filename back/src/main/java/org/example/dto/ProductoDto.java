package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link org.example.model.ProductoEty}
 */
public record ProductoDto(BigDecimal id, @NotNull @Size(max = 255) String nombre, @NotNull BigDecimal precio,
                          @Size(max = 255) String sku, Integer stock) implements Serializable {
}