package org.example.dto;

import java.time.LocalDate;

// record: solo los 3 campos de encabezado que se pueden corregir (numero/fecha/cliente).
// El cliente manda nada mas el que quiere cambiar, ej. {"cliente": "Nuevo nombre"}.
public record FacturaPatchDTO(
        String numero,
        LocalDate fecha,
        String cliente
) {
}
