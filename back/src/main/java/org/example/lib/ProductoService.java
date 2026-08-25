package org.example.lib;

import org.example.dto.ProductoDTO;
import org.example.dto.ProductoPatchDTO;

public interface ProductoService extends Service<ProductoDTO, Long> {

    ProductoDTO actualizar(Long id, ProductoDTO dto);

    // PATCH: reemplazo parcial. A diferencia de actualizar() (PUT), aqui solo se tocan
    // los campos que vienen no-null en el DTO -- ver ProductoPatchDTO.
    ProductoDTO patch(Long id, ProductoPatchDTO cambios);

    boolean eliminar(Long id);
}
