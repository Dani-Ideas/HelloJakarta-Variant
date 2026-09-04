package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.ProductoDto;
import org.example.dto.ProductoPatchDto;
import org.example.lib.ProductoRepository;
import org.example.lib.ProductoService;
import org.example.mapper.ProductoMapper;
import org.example.model.ProductoEty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Inyecta la INTERFAZ ProductoRepository, con @Inject (CDI): ProductoRepository es un
// repositorio Jakarta Data generado por el proveedor, no un @Stateless escrito a mano.
@Stateless
public class ProductoServiceImpl implements ProductoService {

    @Inject
    private ProductoRepository productoRepository;

    // ProductoMapper.INSTANCE, no @Inject: MapStruct con componentModel por default genera
    // una clase normal, instanciable via Mappers.getMapper() -- no un bean CDI.
    private final ProductoMapper productoMapper = ProductoMapper.INSTANCE;

    @Override
    public ProductoDto crear(ProductoDto dto) {
        // Sin EntityManager/flush() aqui: Producto.id usa GenerationType.SEQUENCE, no
        // IDENTITY -- el id se reserva ANTES del INSERT, asi que insert() ya lo devuelve
        // poblado sin forzar nada.
        ProductoEty creado = productoRepository.insert(productoMapper.toEntity(dto));
        return productoMapper.toDto(creado);
    }

    @Override
    public List<ProductoDto> listar() {
        // findAll() de Jakarta Data devuelve Stream<T>, no List<T>.
        return productoRepository.findAll()
                .map(productoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoDto buscarPorId(Long id) {
        return productoMapper.toDto(productoRepository.findById(id).orElse(null));
    }

    @Override
    public ProductoDto actualizar(Long id, ProductoDto dto) {
        // El copiado de campos vive aqui (no en el Repository, Jakarta Data no permite
        // metodos con cuerpo propio en la interfaz) -- es logica de negocio.
        Optional<ProductoEty> existente = productoRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        ProductoEty entidad = existente.get();
        entidad.setNombre(dto.nombre());
        entidad.setSku(dto.sku());
        entidad.setPrecio(dto.precio());
        entidad.setStock(dto.stock());
        ProductoEty actualizado = productoRepository.update(entidad);
        return productoMapper.toDto(actualizado);
    }

    @Override
    public ProductoDto patch(Long id, ProductoPatchDto cambios) {
        // Diferencia con actualizar() (PUT): aqui solo se copia un campo si vino no-null
        // en el DTO. Todo lo que el cliente no haya mandado se queda como estaba.
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
        ProductoEty actualizado = productoRepository.update(entidad);
        return productoMapper.toDto(actualizado);
    }

    @Override
    public boolean eliminar(Long id) {
        // Sin try/catch, sin EntityManager: el conflicto de FK (si lo hay) se detecta en
        // el limite HTTP -- ver rest/EJBExceptionMapper.
        if (productoRepository.findById(id).isEmpty()) {
            return false;
        }
        productoRepository.deleteById(id);
        return true;
    }
}
