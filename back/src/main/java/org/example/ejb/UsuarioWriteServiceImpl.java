package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.example.dto.UsuarioDto;
import org.example.lib.UsuarioRepository;
import org.example.lib.UsuarioWriteService;
import org.example.mapper.UsuarioMapper;
import org.example.model.UsuarioEty;

import java.util.Optional;

@Stateless
public class UsuarioWriteServiceImpl implements UsuarioWriteService {

    @Inject
    private UsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper = UsuarioMapper.INSTANCE;

    // Usuario.id usa GenerationType.IDENTITY -- UsuarioResource.crear() usa creado.id()
    // para el header Location del POST, asi que hace falta el flush() (mismo patron que
    // SesionCajaWriteServiceImpl -- ver Documentation/bitacora-fixes.md incidente #15).
    @PersistenceContext(unitName = "HelloJakartaPU")
    private EntityManager em;

    @Override
    public UsuarioDto crear(UsuarioDto dto) {
        UsuarioEty creado = usuarioRepository.insert(usuarioMapper.toEntity(dto));
        em.flush();
        return usuarioMapper.toDto(creado);
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
        return usuarioMapper.toDto(usuarioRepository.update(entidad));
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
