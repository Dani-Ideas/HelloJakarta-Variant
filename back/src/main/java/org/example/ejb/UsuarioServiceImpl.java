package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.UsuarioDTO;
import org.example.lib.UsuarioRepository;
import org.example.lib.UsuarioService;
import org.example.mapper.UsuarioMapper;
import org.example.model.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Generado por scripts/generar_capas.py siguiendo el mismo patron que ProductoServiceImpl
// (Jakarta Data + MapStruct, sin EntityManager/flush() -- ver Documentation/bitacora-fixes.md
// incidente #15 si Usuario.id usara GenerationType.IDENTITY en vez de SEQUENCE).
@Stateless
public class UsuarioServiceImpl implements UsuarioService {

    @Inject
    private UsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper = UsuarioMapper.INSTANCE;

    @Override
    public UsuarioDTO crear(UsuarioDTO dto) {
        Usuario creado = usuarioRepository.insert(usuarioMapper.toEntity(dto));
        return usuarioMapper.toDTO(creado);
    }

    @Override
    public List<UsuarioDTO> listar() {
        return usuarioRepository.findAll()
                .map(usuarioMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UsuarioDTO buscarPorId(Long id) {
        return usuarioMapper.toDTO(usuarioRepository.findById(id).orElse(null));
    }

    @Override
    public UsuarioDTO actualizar(Long id, UsuarioDTO dto) {
        Optional<Usuario> existente = usuarioRepository.findById(id);
        if (existente.isEmpty()) {
            return null;
        }
        Usuario entidad = existente.get();
        entidad.setNombre(dto.nombre());
        entidad.setRol(dto.rol());
        Usuario actualizado = usuarioRepository.update(entidad);
        return usuarioMapper.toDTO(actualizado);
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
