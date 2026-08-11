package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class FacturaDetalleDTO {

    private Long id;

    @NotNull(message = "El id del producto es obligatorio")
    private Long productoId;

    private String nombreProducto;

    @Positive(message = "La cantidad debe ser mayor a 0")
    private int cantidad;

    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}
