package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.ProductoDTO;
import org.example.dto.ProductoPatchDTO;
import org.example.lib.ProductoRepository;
import org.example.lib.ProductoService;
import org.example.mapper.ProductoMapper;
import org.example.model.Producto;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Inyecta la INTERFAZ ProductoRepository, igual que antes -- pero ahora con @Inject (CDI),
// no @EJB: ProductoRepository ya no es un bean @Stateless escrito a mano, es un repositorio
// Jakarta Data generado por el proveedor, y esos son beans CDI, no EJB.
@Stateless
public class ProductoServiceImpl implements ProductoService {

    @Inject
    private ProductoRepository productoRepository;

    // ProductoMapper.INSTANCE, no @Inject: MapStruct con componentModel por default genera
    // una clase normal, instanciable via Mappers.getMapper() -- no un bean CDI. INSTANCE
    // es el patron estandar de MapStruct para tener un unico mapper reutilizable sin DI.
    private final ProductoMapper productoMapper = ProductoMapper.INSTANCE;

    // EntityManager inyectado SOLO para forzar el flush tras insert(): el repositorio
    // Jakarta Data generado por el proveedor no sincroniza el id IDENTITY en el objeto
    // que devuelve (confirmado -- sin este flush, "creado.getId()" viene null). Este
    // EntityManager SI comparte el mismo contexto de persistencia que usa el repositorio
    // generado, porque ambos participan de la misma transaccion JTA y la misma unidad de
    // persistencia (HelloJakartaPU) -- por eso el flush de aqui sincroniza el id alla.
    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    @Override
    public ProductoDTO crear(ProductoDTO dto) {
        Producto creado = productoRepository.insert(productoMapper.toEntity(dto));
        em.flush();
        return productoMapper.toDTO(creado);
    }

    @Override
    public List<ProductoDTO> listar() {
        // findAll() de Jakarta Data devuelve Stream<T>, no List<T> como el findAll()
        // "casero" que teniamos antes -- por eso aqui ya no hace falta .stream().
        return productoRepository.findAll()
                .map(productoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoDTO buscarPorId(Long id) {
        return productoMapper.toDTO(productoRepository.findById(id).orElse(null));
    }

    @Override
    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        // El copiado de campos ya no vive en el Repository (Jakarta Data no te deja
        // escribir metodos con cuerpo propio en la interfaz de repositorio) -- es logica
        // de negocio ("que significa actualizar un Producto"), asi que vive aqui.
        Optional<Producto> existente = productoRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        Producto entidad = existente.get();
        entidad.setNombre(dto.nombre());
        entidad.setSku(dto.sku());
        entidad.setPrecio(dto.precio());
        entidad.setStock(dto.stock());
        Producto actualizado = productoRepository.update(entidad);
        return productoMapper.toDTO(actualizado);
    }

    @Override
    public ProductoDTO patch(Long id, ProductoPatchDTO cambios) {
        // Diferencia con actualizar() (PUT): aqui solo se copia un campo si vino no-null
        // en el DTO. Todo lo que el cliente no haya mandado se queda como estaba.
        Optional<Producto> existente = productoRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        Producto entidad = existente.get();
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
        Producto actualizado = productoRepository.update(entidad);
        return productoMapper.toDTO(actualizado);
    }

    @Override
    public boolean eliminar(Long id) {
        if (productoRepository.findById(id).isEmpty()) {
            return false;
        }
        productoRepository.deleteById(id);
        return true;
    }
}
