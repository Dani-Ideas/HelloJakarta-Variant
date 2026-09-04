package org.example.lib;

import org.example.dto.FacturaDto;
import org.example.dto.FacturaPatchDto;

public interface FacturaService extends Service<FacturaDto, Long> {

    FacturaDto actualizar(Long id, FacturaDto dto);

    // PATCH: solo corrige los campos de encabezado que vengan no-null en el DTO -- ver
    // FacturaPatchDto. No requiere reenviar numero/fecha/cliente/detalles completos.
    FacturaDto patch(Long id, FacturaPatchDto cambios);
}
