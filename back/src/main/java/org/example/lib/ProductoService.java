package org.example.lib;

import org.example.dto.ProductoDTO;

public interface ProductoService extends Service<ProductoDTO, Long> {

    ProductoDTO actualizar(Long id, ProductoDTO dto);

    boolean eliminar(Long id);
}
