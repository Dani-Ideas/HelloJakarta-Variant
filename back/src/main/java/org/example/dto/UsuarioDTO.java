package org.example.dto;

import org.example.model.Rol;
// Generado por scripts/generar_capas.py a partir de model/Usuario.java -- revisa que
// los tipos importados sean correctos y AGREGA las validaciones (@NotBlank, @NotNull,
// @Positive, etc.) que correspondan a las reglas de negocio reales de Usuario, no se
// adivinan solas (ver ProductoDTO para el patron de como se ven esas anotaciones).
public record UsuarioDTO(
        Long id,
        String nombre,
        Rol rol
) {
}
