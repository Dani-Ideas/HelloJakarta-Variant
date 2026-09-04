package org.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import org.example.dto.UsuarioDto;
import org.example.lib.UsuarioReadService;
import org.example.lib.UsuarioRepository;
import org.example.mapper.UsuarioMapper;

import java.util.List;
import java.util.stream.Collectors;

@Stateless
public class UsuarioReadServiceImpl implements UsuarioReadService {

    @Inject
    private UsuarioRepository usuarioRepository;

    private final UsuarioMapper usuarioMapper = UsuarioMapper.INSTANCE;

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
}
