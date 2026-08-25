package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.ProductoDTO;
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
        Producto creado = productoRepository.insert(ProductoMapper.toEntity(dto));
        em.flush();
        return ProductoMapper.toDTO(creado);
    }

    @Override
    public List<ProductoDTO> listar() {
        // findAll() de Jakarta Data devuelve Stream<T>, no List<T> como el findAll()
        // "casero" que teniamos antes -- por eso aqui ya no hace falta .stream().
        return productoRepository.findAll()
                .map(ProductoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoDTO buscarPorId(Long id) {
        return ProductoMapper.toDTO(productoRepository.findById(id).orElse(null));
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
        entidad.setNombre(dto.getNombre());
        entidad.setSku(dto.getSku());
        entidad.setPrecio(dto.getPrecio());
        entidad.setStock(dto.getStock());
        Producto actualizado = productoRepository.update(entidad);
        return ProductoMapper.toDTO(actualizado);
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
