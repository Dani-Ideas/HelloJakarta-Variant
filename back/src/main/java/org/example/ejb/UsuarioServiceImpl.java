package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.UsuarioDto;
import org.example.lib.UsuarioRepository;
import org.example.lib.UsuarioService;
import org.example.mapper.UsuarioMapper;
import org.example.model.UsuarioEty;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Stateless
public class UsuarioServiceImpl implements UsuarioService {

    @Inject
    private UsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper = UsuarioMapper.INSTANCE;

    // Usuario.id usa GenerationType.IDENTITY -- UsuarioResource.crear() usa creado.id()
    // para el header Location del POST, asi que si hace falta el flush() (mismo patron
    // que SesionCajaServiceImpl -- ver Documentation/bitacora-fixes.md incidente #15).
    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    @Override
    public UsuarioDto crear(UsuarioDto dto) {
        UsuarioEty creado = usuarioRepository.insert(usuarioMapper.toEntity(dto));
        em.flush();
        return usuarioMapper.toDto(creado);
    }

    @Override
    public List<UsuarioDto> listar() {
        return usuarioRepository.findAll()
                .map(usuarioMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDto buscarPorId(Long id) {
        return usuarioMapper.toDto(usuarioRepository.findById(id).orElse(null));
    }

    @Override
    public UsuarioDto actualizar(Long id, UsuarioDto dto) {
        Optional<UsuarioEty> existente = usuarioRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        UsuarioEty entidad = existente.get();
        entidad.setNombre(dto.nombre());
        entidad.setRol(dto.rol());
        UsuarioEty actualizado = usuarioRepository.update(entidad);
        return usuarioMapper.toDto(actualizado);
    }

    @Override
    public boolean eliminar(Long id) {
        if (usuarioRepository.findById(id).isEmpty()) {
            return false;
        }
        usuarioRepository.deleteById(id);
        return true;
    }
}
