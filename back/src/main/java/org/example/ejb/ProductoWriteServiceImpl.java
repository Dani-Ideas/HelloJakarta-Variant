package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.ProductoDto;
import org.example.dto.ProductoPatchDto;
import org.example.lib.ProductoRepository;
import org.example.lib.ProductoWriteService;
import org.example.mapper.ProductoMapper;
import org.example.model.ProductoEty;

import java.util.Optional;

// Solo atiende escritura -- crear/actualizar/patch/eliminar.
@Stateless
public class ProductoWriteServiceImpl implements ProductoWriteService {

    @Inject
    private ProductoRepository productoRepository;

    private final ProductoMapper productoMapper = ProductoMapper.INSTANCE;

    @Override
    public ProductoDto crear(ProductoDto dto) {
        // Sin EntityManager/flush(): Producto.id usa SEQUENCE, no IDENTITY.
        ProductoEty creado = productoRepository.insert(productoMapper.toEntity(dto));
        return productoMapper.toDto(creado);
    }

    @Override
    public ProductoDto actualizar(Long id, ProductoDto dto) {
        Optional<ProductoEty> existente = productoRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        ProductoEty entidad = existente.get();
        entidad.setNombre(dto.nombre());
        entidad.setSku(dto.sku());
        entidad.setPrecio(dto.precio());
        entidad.setStock(dto.stock());
        return productoMapper.toDto(productoRepository.update(entidad));
    }

    @Override
    public ProductoDto patch(Long id, ProductoPatchDto cambios) {
        Optional<ProductoEty> existente = productoRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        ProductoEty entidad = existente.get();
        if (cambios.nombre() != null) {
            entidad.setNombre(cambios.nombre());
        }
        if (cambios.sku() != null) {
            entidad.setSku(cambios.sku());
        }
        if (cambios.precio() != null) {
            entidad.setPrecio(cambios.precio());
        }
        if (cambios.stock() != null) {
            entidad.setStock(cambios.stock());
        }
        return productoMapper.toDto(productoRepository.update(entidad));
    }

    @Override
    public boolean eliminar(Long id) {
        // Sin try/catch: el conflicto de FK se detecta en rest/EJBExceptionMapper.
        if (productoRepository.findById(id).isEmpty()) {
            return false;
        }
        productoRepository.deleteById(id);
        return true;
    }
}
