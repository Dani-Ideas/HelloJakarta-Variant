package org.example.lib;

import org.example.dto.UsuarioDTO;

public interface UsuarioService extends Service<UsuarioDTO, Long> {

    UsuarioDTO actualizar(Long id, UsuarioDTO dto);

    boolean eliminar(Long id);
}
