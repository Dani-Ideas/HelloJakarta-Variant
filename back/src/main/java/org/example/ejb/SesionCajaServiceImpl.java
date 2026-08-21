package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import org.example.dto.SesionCajaDTO;
import org.example.lib.SesionCajaRepository;
import org.example.lib.SesionCajaService;
import org.example.mapper.SesionCajaMapper;
import org.example.model.SesionCaja;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class SesionCajaServiceImpl implements SesionCajaService {

    @EJB
    private SesionCajaRepository sesionCajaRepository;

    @Override
    public SesionCajaDTO crear(SesionCajaDTO dto) {
        SesionCaja sesion = SesionCajaMapper.toEntity(dto);
        sesion.setFApertura(LocalDateTime.now());   // el servidor decide la hora, no el cliente
        sesion.setCerrada(false);
        SesionCaja creada = sesionCajaRepository.crear(sesion);
        return SesionCajaMapper.toDTO(creada);
    }

    @Override
    public List<SesionCajaDTO> listar() {
        return sesionCajaRepository.listar().stream()
                .map(SesionCajaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SesionCajaDTO buscarPorId(Long id) {
        return SesionCajaMapper.toDTO(sesionCajaRepository.buscarPorId(id));
    }
}
