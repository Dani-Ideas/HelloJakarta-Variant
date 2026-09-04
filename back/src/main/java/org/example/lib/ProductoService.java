package org.example.lib;

import org.example.dto.ProductoDto;
import org.example.dto.ProductoPatchDto;

public interface ProductoService extends Service<ProductoDto, Long> {

    ProductoDto actualizar(Long id, ProductoDto dto);

    // PATCH: reemplazo parcial. A diferencia de actualizar() (PUT), aqui solo se tocan
    // los campos que vienen no-null en el DTO -- ver ProductoPatchDTO.
    ProductoDto patch(Long id, ProductoPatchDto cambios);

    boolean eliminar(Long id);
}
