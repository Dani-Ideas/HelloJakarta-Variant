package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.lib.SesionCajaRepository;
import org.example.lib.SesionCajaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class SesionCajaServiceImpl implements SesionCajaService {

    // @Inject, no @EJB: SesionCajaRepository ahora es un repositorio Jakarta Data
    // (bean CDI, generado por el proveedor) -- ya no un @Stateless escrito a mano.
    @Inject
    private SesionCajaRepository sesionCajaRepository;

    // Ver ProductoServiceImpl/FacturaServiceImpl: insert() no sincroniza el id IDENTITY
    // en el objeto que devuelve -- este flush lo fuerza, comparte el mismo contexto de
    // persistencia (misma transaccion JTA, misma unidad HelloJakartaPU).
    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    @Override
    public SesionCajaDTO crear(SesionCajaDTO dto) {
        SesionCaja sesion = SesionCajaMapper.toEntity(dto);
        sesion.setFApertura(LocalDateTime.now());   // el servidor decide la hora, no el cliente
        sesion.setCerrada(false);
        SesionCaja creada = sesionCajaRepository.insert(sesion);
        em.flush();
        return SesionCajaMapper.toDTO(creada);
    }

    @Override
    public List<SesionCajaDTO> listar() {
        // findAll() de Jakarta Data devuelve Stream<T>, no List<T>.
        return sesionCajaRepository.findAll()
                .map(SesionCajaMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SesionCajaDTO buscarPorId(Long id) {
        return SesionCajaMapper.toDTO(sesionCajaRepository.findById(id).orElse(null));
    }
}
