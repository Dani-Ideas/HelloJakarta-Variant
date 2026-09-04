package org.example.lib;

import org.example.dto.ProductoDto;

public interface FacturaService extends Service<ProductoDto, Long> {

    ProductoDto actualizar(Long id, ProductoDto dto);

    // PATCH: solo corrige los campos de encabezado que vengan no-null en el DTO --
    // ver FacturaPatchDTO. No requiere reenviar numero/fecha/cliente/detalles completos.
    ProductoDto patch(Long id, ProductoDto cambios);
}
