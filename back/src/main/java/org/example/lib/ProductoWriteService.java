package org.example.lib;

import org.example.dto.ProductoDto;
import org.example.dto.ProductoPatchDto;

public interface ProductoWriteService extends WriteService<ProductoDto, Long> {

    ProductoDto actualizar(Long id, ProductoDto dto);

    ProductoDto patch(Long id, ProductoPatchDto cambios);

    boolean eliminar(Long id);
}
