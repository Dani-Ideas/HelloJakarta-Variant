package org.example.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.example.dto.SesionCajaDto;
import org.example.lib.SesionCajaReadService;
import org.example.lib.SesionCajaRepository;
import org.example.mapper.SesionCajaMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// @Singleton (ya NO @Stateless): mismo patron que Producto/Usuario -- SesionCaja no tiene
// FK saliente (es Factura quien la referencia a ella, no al reves), asi que es "tabla base".
// Sin @DependsOn: DatosIniciales no siembra SesionCaja.
@Singleton
@Startup
public class SesionCajaReadServiceImpl implements SesionCajaReadService {

    @Inject
    private SesionCajaRepository sesionCajaRepository;

    private final SesionCajaMapper sesionCajaMapper = SesionCajaMapper.INSTANCE;

    private final Map<Long, SesionCajaDto> cache = new ConcurrentHashMap<>();

    @PostConstruct
    private void precargarCache() {
        sesionCajaRepository.findAll()
                .map(sesionCajaMapper::toDto)
                .forEach(dto -> cache.put(dto.id(), dto));
    }

    @Override
    @Lock(LockType.READ)
    public List<SesionCajaDto> listar() {
        return List.copyOf(cache.values());
    }

    @Override
    @Lock(LockType.READ)
    public SesionCajaDto buscarPorId(Long id) {
        return cache.get(id);
    }

    @Override
    @Lock(LockType.WRITE)
    public void refrescarCache(SesionCajaDto dto) {
        cache.put(dto.id(), dto);
    }

    @Override
    @Lock(LockType.WRITE)
    public void quitarDeCache(Long id) {
        cache.remove(id);
    }
}
