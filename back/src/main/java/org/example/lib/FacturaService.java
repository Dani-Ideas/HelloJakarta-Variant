package org.example.lib;

import org.example.dto.FacturaDTO;

public interface FacturaService extends Service<FacturaDTO, Long> {

    FacturaDTO actualizar(Long id, FacturaDTO dto);
}
