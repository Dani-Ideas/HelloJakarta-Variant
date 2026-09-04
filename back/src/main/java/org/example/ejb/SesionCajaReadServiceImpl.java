package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.SesionCajaDto;
import org.example.lib.SesionCajaReadService;
import org.example.lib.SesionCajaRepository;
import org.example.mapper.SesionCajaMapper;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class SesionCajaReadServiceImpl implements SesionCajaReadService {

    @Inject
    private SesionCajaRepository sesionCajaRepository;

    private final SesionCajaMapper sesionCajaMapper = SesionCajaMapper.INSTANCE;

    @Override
    public List<SesionCajaDto> listar() {
        return sesionCajaRepository.findAll()
                .map(sesionCajaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SesionCajaDto buscarPorId(Long id) {
        return sesionCajaMapper.toDto(sesionCajaRepository.findById(id).orElse(null));
    }
}
