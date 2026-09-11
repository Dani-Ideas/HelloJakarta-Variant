package org.example.lib;

import org.example.dto.UsuarioDto;

// refrescarCache/quitarDeCache: mismo patron que ProductoReadService -- no son API publica,
// solo para que UsuarioWriteServiceImpl avise a este mismo bean (@Singleton con cache) cuando
// cambia un Usuario.
public interface UsuarioReadService extends ReadService<UsuarioDto, Long> {

    void refrescarCache(UsuarioDto dto);

    void quitarDeCache(Long id);
}
