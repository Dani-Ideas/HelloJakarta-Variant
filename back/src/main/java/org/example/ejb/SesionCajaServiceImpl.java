package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.SesionCajaDto;
import org.example.lib.SesionCajaRepository;
import org.example.lib.SesionCajaService;
import org.example.mapper.SesionCajaMapper;
import org.example.model.SesionCajaEty;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class SesionCajaServiceImpl implements SesionCajaService {

    // @Inject, no @EJB: SesionCajaRepository es un repositorio Jakarta Data (bean CDI,
    // generado por el proveedor) -- ya no un @Stateless escrito a mano.
    @Inject
    private SesionCajaRepository sesionCajaRepository;

    private final SesionCajaMapper sesionCajaMapper = SesionCajaMapper.INSTANCE;

    // SesionCaja usa GenerationType.IDENTITY (a diferencia de Producto/Factura, que usan
    // SEQUENCE) -- con IDENTITY, insert() no sincroniza el id en el objeto que devuelve
    // hasta que se fuerza el flush(). Ver Documentation/bitacora-fixes.md incidente #15.
    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    @Override
    public SesionCajaDto crear(SesionCajaDto dto) {
        SesionCajaEty sesion = sesionCajaMapper.toEntity(dto);
        sesion.setFApertura(LocalDateTime.now());   // el servidor decide la hora, no el cliente
        sesion.setCerrada(false);
        SesionCajaEty creada = sesionCajaRepository.insert(sesion);
        em.flush();
        return sesionCajaMapper.toDto(creada);
    }

    @Override
    public List<SesionCajaDto> listar() {
        // findAll() de Jakarta Data devuelve Stream<T>, no List<T>.
        return sesionCajaRepository.findAll()
                .map(sesionCajaMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SesionCajaDto buscarPorId(Long id) {
        return sesionCajaMapper.toDto(sesionCajaRepository.findById(id).orElse(null));
    }
}
