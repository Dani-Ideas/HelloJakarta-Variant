package org.example.lib;

import org.example.dto.FacturaDTO;
import org.example.dto.FacturaPatchDTO;

public interface FacturaService extends Service<FacturaDTO, Long> {

    FacturaDTO actualizar(Long id, FacturaDTO dto);

    // PATCH: solo corrige los campos de encabezado que vengan no-null en el DTO --
    // ver FacturaPatchDTO. No requiere reenviar numero/fecha/cliente/detalles completos.
    FacturaDTO patch(Long id, FacturaPatchDTO cambios);
}
