package org.example.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class FacturaDTO {

    private Long id;

    @NotBlank(message = "El numero de factura es obligatorio")
    private String numero;

    private LocalDate fecha;

    @NotBlank(message = "El cliente es obligatorio")
    private String cliente;

    private BigDecimal total;

    @NotEmpty(message = "La factura debe tener al menos un detalle")
    @Valid
    private List<FacturaDetalleDTO> detalles;
}
