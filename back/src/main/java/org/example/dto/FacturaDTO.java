package org.example.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FacturaDTO(
        Long id,

        @NotBlank(message = "El numero de factura es obligatorio")
        String numero,

        LocalDate fecha,

        @NotBlank(message = "El cliente es obligatorio")
        String cliente,

        BigDecimal total,

        @NotEmpty(message = "La factura debe tener al menos un detalle")
        @Valid
        List<FacturaDetalleDTO> detalles
) {
}
