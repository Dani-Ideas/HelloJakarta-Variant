package org.example.lib;

import org.example.dto.FacturaDto;
import org.example.dto.FacturaPatchDto;

// Sin eliminar() a proposito: borrar una factura ya emitida no tiene sentido de negocio.
public interface FacturaWriteService extends WriteService<FacturaDto, Long> {

    FacturaDto actualizar(Long id, FacturaDto dto);

    FacturaDto patch(Long id, FacturaPatchDto cambios);
}
