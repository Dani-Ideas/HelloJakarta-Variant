package org.example.mapper;

import org.example.dto.UsuarioDto;
import org.example.model.UsuarioEty;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

// MapStruct genera UsuarioMapperImpl en tiempo de compilacion (revisa
// target/generated-sources/annotations despues de compilar) -- los campos que coinciden
// de nombre y tipo entre Usuario y UsuarioDTO se mapean solos, sin declarar nada.
@Mapper
public interface UsuarioMapper {

    UsuarioMapper INSTANCE = Mappers.getMapper(UsuarioMapper.class);
    // id se ignora a proposito: una entidad nueva nunca debe nacer con el id que
    // (si acaso) mando el cliente en el JSON -- lo genera la base de datos.
    @Mapping(target = "id", ignore = true)
    UsuarioEty toEntity(UsuarioDto usuarioDto);

    UsuarioDto toDto(UsuarioEty usuarioEty);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UsuarioEty partialUpdate(UsuarioDto usuarioEtyDto, @MappingTarget UsuarioEty usuarioEty);
}
