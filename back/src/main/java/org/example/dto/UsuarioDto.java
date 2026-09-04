package org.example.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.model.Rol;

import java.io.Serializable;

/**
 * DTO for {@link org.example.model.UsuarioEty}
 */
public record UsuarioDto(
        Long id,
        @NotNull @Size(max = 255) String nombre,
        Rol rol
) implements Serializable {
}
