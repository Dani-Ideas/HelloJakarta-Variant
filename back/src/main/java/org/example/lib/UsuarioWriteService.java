package org.example.lib;

import org.example.dto.UsuarioDto;

public interface UsuarioWriteService extends WriteService<UsuarioDto, Long> {

    UsuarioDto actualizar(Long id, UsuarioDto dto);

    boolean eliminar(Long id);
}
