package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.ProductoDto;
import org.example.lib.ProductoReadService;
import org.example.lib.ProductoRepository;
import org.example.mapper.ProductoMapper;

import java.util.List;
import java.util.stream.Collectors;

// Solo atiende consultas -- listar/buscarPorId. Comparte el mismo ProductoMapper.INSTANCE
// que usa ProductoWriteServiceImpl (los mappers no se dividen por lectura/escritura, son
// los mismos para ambos).
@Stateless
public class ProductoReadServiceImpl implements ProductoReadService {

    @Inject
    private ProductoRepository productoRepository;

    private final ProductoMapper productoMapper = ProductoMapper.INSTANCE;

    @Override
    public List<ProductoDto> listar() {
        return productoRepository.findAll()
                .map(productoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ProductoDto buscarPorId(Long id) {
        return productoMapper.toDto(productoRepository.findById(id).orElse(null));
    }
}
