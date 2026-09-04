package org.example.lib;

import org.example.dto.SesionCajaDto;

// Solo crear() (de WriteService) -- SesionCaja no expone actualizar/patch/eliminar por REST.
public interface SesionCajaWriteService extends WriteService<SesionCajaDto, Long> {
}
