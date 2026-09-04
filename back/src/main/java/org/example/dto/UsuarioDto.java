package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link org.example.model.UsuarioEty}
 */
public record UsuarioDto(BigDecimal id, @NotNull @Size(max = 255) String nombre,
                         @Size(max = 255) String rol) implements Serializable {
}