package org.example.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for {@link org.example.model.FacturaEty}
 */
public record FacturaDto(
        Long id,
        @Size(max = 255) String cliente,
        LocalDate fecha,
        @NotNull @Size(max = 255) String numero,
        BigDecimal total,
        // detalles: se habia perdido en la regeneracion -- sin esto una Factura no podia
        // mostrar sus lineas. Sin @NotEmpty al crear no aplica aqui porque se valida en
        // el Resource via @Valid; se deja NotEmpty porque una factura sin lineas no
        // tiene sentido de negocio.
        @NotEmpty @Valid List<FacturaDetalleDto> detalles
) implements Serializable {
}
