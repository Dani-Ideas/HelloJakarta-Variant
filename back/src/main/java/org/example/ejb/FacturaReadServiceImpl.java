package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.FacturaDto;
import org.example.lib.FacturaReadService;
import org.example.lib.FacturaRepository;
import org.example.mapper.FacturaMapper;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class FacturaReadServiceImpl implements FacturaReadService {

    @Inject
    private FacturaRepository facturaRepository;

    private final FacturaMapper facturaMapper = FacturaMapper.INSTANCE;

    @Override
    public List<FacturaDto> listar() {
        return facturaRepository.findAll()
                .map(facturaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public FacturaDto buscarPorId(Long id) {
        return facturaMapper.toDto(facturaRepository.findById(id).orElse(null));
    }
}
