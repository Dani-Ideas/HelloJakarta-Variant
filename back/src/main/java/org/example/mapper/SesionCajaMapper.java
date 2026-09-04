package org.example.mapper;

import org.example.dto.SesionCajaDto;
import org.example.model.SesionCajaEty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SesionCajaMapper {

    SesionCajaMapper INSTANCE = Mappers.getMapper(SesionCajaMapper.class);

    SesionCajaDto toDto(SesionCajaEty sesion);

    // id, cerrada y fApertura se ignoran: los decide el servidor al crear (ver
    // SesionCajaServiceImpl.crear()), no el cliente.
    @Mapping(target = "id", ignore = true)
    SesionCajaEty toEntity(SesionCajaDto dto);
}
