package org.example.lib;

import org.example.dto.UsuarioDto;

public interface UsuarioService extends Service<UsuarioDto, Long> {

    UsuarioDto actualizar(Long id, UsuarioDto dto);

    boolean eliminar(Long id);
}
