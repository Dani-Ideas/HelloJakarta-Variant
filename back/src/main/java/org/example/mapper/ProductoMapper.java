package org.example.mapper;

import org.example.dto.ProductoDTO;
import org.example.model.Producto;

public final class ProductoMapper {

    private ProductoMapper() {
    }

    public static ProductoDTO toDTO(Producto producto) {
        if (producto == null) {
            return null;
        }
        return new ProductoDTO(
                producto.getId(),
                producto.getNombre(),
                producto.getSku(),
                producto.getPrecio(),
                producto.getStock()
        );
    }

    public static Producto toEntity(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setSku(dto.getSku());
        producto.setPrecio(dto.getPrecio());
        producto.setStock(dto.getStock());
        return producto;
    }
}
