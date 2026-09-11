package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.ProductoDto;
import org.example.dto.ProductoPatchDto;
import org.example.lib.ProductoReadService;
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

    // ProductoReadServiceImpl ahora es @Singleton con cache en memoria (ver esa clase) --
    // cada metodo de aqui abajo que cambia la base de datos tiene que avisarle, si no el
    // cache se queda con datos viejos indefinidamente (nadie mas lo refresca solo).
    @EJB
    private ProductoReadService productoReadService;

    private final ProductoMapper productoMapper = ProductoMapper.INSTANCE;

    @Override
    public ProductoDto crear(ProductoDto dto) {
        // Sin EntityManager/flush(): Producto.id usa SEQUENCE, no IDENTITY.
        ProductoEty creado = productoRepository.insert(productoMapper.toEntity(dto));
        ProductoDto creadoDto = productoMapper.toDto(creado);
        productoReadService.refrescarCache(creadoDto);
        return creadoDto;
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
        ProductoDto actualizadoDto = productoMapper.toDto(productoRepository.update(entidad));
        productoReadService.refrescarCache(actualizadoDto);
        return actualizadoDto;
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
        ProductoDto actualizadoDto = productoMapper.toDto(productoRepository.update(entidad));
        productoReadService.refrescarCache(actualizadoDto);
        return actualizadoDto;
    }

    @Override
    public boolean eliminar(Long id) {
        // Sin try/catch: el conflicto de FK se detecta en rest/EJBExceptionMapper.
        if (productoRepository.findById(id).isEmpty()) {
            return false;
        }
        productoRepository.deleteById(id);
        productoReadService.quitarDeCache(id);
        return true;
    }
}
