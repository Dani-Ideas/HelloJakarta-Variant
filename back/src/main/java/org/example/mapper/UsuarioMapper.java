package org.example.mapper;

import org.example.dto.UsuarioDto;
import org.example.model.UsuarioEty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UsuarioMapper {

    UsuarioMapper INSTANCE = Mappers.getMapper(UsuarioMapper.class);

    UsuarioDto toDto(UsuarioEty usuario);

    @Mapping(target = "id", ignore = true)
    UsuarioEty toEntity(UsuarioDto dto);
}
