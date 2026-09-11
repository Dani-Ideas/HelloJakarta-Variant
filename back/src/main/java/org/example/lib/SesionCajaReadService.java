package org.example.lib;

import org.example.dto.SesionCajaDto;

// refrescarCache/quitarDeCache: mismo patron que ProductoReadService/UsuarioReadService --
// solo para que SesionCajaWriteServiceImpl avise a este mismo bean (@Singleton con cache).
// quitarDeCache queda por simetria aunque SesionCaja no expone eliminar por REST hoy.
public interface SesionCajaReadService extends ReadService<SesionCajaDto, Long> {

    void refrescarCache(SesionCajaDto dto);

    void quitarDeCache(Long id);
}
