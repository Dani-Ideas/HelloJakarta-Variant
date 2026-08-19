package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dto.ProductoDTO;
import org.example.lib.ProductoRepository;
import org.example.lib.ProductoService;
import org.example.mapper.ProductoMapper;
import org.example.model.Producto;

import java.util.List;
import java.util.stream.Collectors;

// Inyecta la INTERFAZ ProductoRepository, nunca ProductoRepositoryImpl -- este bean no
// sabe (ni necesita saber) que existe esa clase concreta.
@Stateless
public class ProductoServiceImpl implements ProductoService {

    @EJB
    private ProductoRepository productoRepository;

    @Override
    public ProductoDTO crear(ProductoDTO dto) {
        Producto creado = productoRepository.crear(ProductoMapper.toEntity(dto));
        return ProductoMapper.toDTO(creado);
    }

    @Override
    public List<ProductoDTO> listar() {
        return productoRepository.listar().stream()
                .map(ProductoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoDTO buscarPorId(Long id) {
        return ProductoMapper.toDTO(productoRepository.buscarPorId(id));
    }

    @Override
    public ProductoDTO actualizar(Long id, ProductoDTO dto) {
        Producto actualizado = productoRepository.actualizar(id, ProductoMapper.toEntity(dto));
        return actualizado == null ? null : ProductoMapper.toDTO(actualizado);
    }

    @Override
    public boolean eliminar(Long id) {
        return productoRepository.eliminar(id);
    }
}
