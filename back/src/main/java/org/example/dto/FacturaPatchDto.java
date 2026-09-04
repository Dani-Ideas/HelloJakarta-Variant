package org.example.dto;

import java.io.Serializable;
import java.time.LocalDate;

// Solo los 3 campos de encabezado que se pueden corregir -- el cliente manda nada mas el
// que quiere cambiar, ej. {"cliente": "Nuevo nombre"}.
public record FacturaPatchDto(
        String numero,
        LocalDate fecha,
        String cliente
) implements Serializable {
}
