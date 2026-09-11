package org.example.ejb;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.SesionCajaDto;
import org.example.lib.SesionCajaReadService;
import org.example.lib.SesionCajaRepository;
import org.example.lib.SesionCajaWriteService;
import org.example.mapper.SesionCajaMapper;
import org.example.model.SesionCajaEty;

import java.time.LocalDateTime;

@Stateless
public class SesionCajaWriteServiceImpl implements SesionCajaWriteService {

    @Inject
    private SesionCajaRepository sesionCajaRepository;

    // SesionCajaReadServiceImpl ahora es @Singleton con cache -- avisarle en cada
    // escritura, mismo patron que Producto/Usuario.
    @EJB
    private SesionCajaReadService sesionCajaReadService;

    private final SesionCajaMapper sesionCajaMapper = SesionCajaMapper.INSTANCE;

    // SesionCaja usa GenerationType.IDENTITY -- con IDENTITY, insert() no sincroniza el id
    // en el objeto que devuelve hasta que se fuerza el flush(). Ver
    // Documentation/bitacora-fixes.md incidente #15.
    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    @Override
    public SesionCajaDto crear(SesionCajaDto dto) {
        SesionCajaEty sesion = sesionCajaMapper.toEntity(dto);
        sesion.setFApertura(LocalDateTime.now());   // el servidor decide la hora, no el cliente
        sesion.setCerrada(false);
        SesionCajaEty creada = sesionCajaRepository.insert(sesion);
        em.flush();
        SesionCajaDto creadaDto = sesionCajaMapper.toDto(creada);
        sesionCajaReadService.refrescarCache(creadaDto);
        return creadaDto;
    }
}
