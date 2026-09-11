package org.example.ejb;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import org.example.dto.UsuarioDto;
import org.example.lib.UsuarioReadService;
import org.example.lib.UsuarioRepository;
import org.example.mapper.UsuarioMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// @Singleton (ya NO @Stateless): mismo patron que ProductoReadServiceImpl -- Usuario es una
// "tabla base" (sin FK saliente) que se escribe poco y se lee mucho. Sin @DependsOn aqui
// porque DatosIniciales no siembra Usuario (solo Producto) -- no hay orden que forzar.
@Singleton
@Startup
public class UsuarioReadServiceImpl implements UsuarioReadService {

    @Inject
    private UsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper = UsuarioMapper.INSTANCE;

    private final Map<Long, UsuarioDto> cache = new ConcurrentHashMap<>();

    @PostConstruct
    private void precargarCache() {
        usuarioRepository.findAll()
                .map(usuarioMapper::toDto)
                .forEach(dto -> cache.put(dto.id(), dto));
    }

    @Override
    @Lock(LockType.READ)
    public List<UsuarioDto> listar() {
        return List.copyOf(cache.values());
    }

    @Override
    @Lock(LockType.READ)
    public UsuarioDto buscarPorId(Long id) {
        return cache.get(id);
    }

    @Override
    @Lock(LockType.WRITE)
    public void refrescarCache(UsuarioDto dto) {
        cache.put(dto.id(), dto);
    }

    @Override
    @Lock(LockType.WRITE)
    public void quitarDeCache(Long id) {
        cache.remove(id);
    }
}
