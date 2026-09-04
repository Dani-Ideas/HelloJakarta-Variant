package org.example.mapper;

import org.example.dto.ProductoDto;
import org.example.model.ProductoEty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ProductoMapper {

    ProductoMapper INSTANCE = Mappers.getMapper(ProductoMapper.class);

    ProductoDto toDto(ProductoEty producto);

    // id se ignora a proposito: un Producto nuevo nunca debe nacer con el id que (si
    // acaso) mando el cliente en el JSON -- lo genera la base de datos.
    @Mapping(target = "id", ignore = true)
    ProductoEty toEntity(ProductoDto dto);
}
