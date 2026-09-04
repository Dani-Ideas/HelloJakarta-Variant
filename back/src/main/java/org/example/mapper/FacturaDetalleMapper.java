package org.example.mapper;

import org.example.dto.FacturaDetalleDto;
import org.example.model.FacturaDetalleEty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

// uses = ProductoMapper: para mapear el campo anidado "producto" (FacturaDetalleEty.producto
// <-> FacturaDetalleDto.producto), MapStruct necesita otro Mapper que sepa convertir
// ProductoEty <-> ProductoDto -- se lo declara aqui.
@Mapper(uses = ProductoMapper.class)
public interface FacturaDetalleMapper {

    FacturaDetalleMapper INSTANCE = Mappers.getMapper(FacturaDetalleMapper.class);

    FacturaDetalleDto toDto(FacturaDetalleEty detalle);

    // id y factura se ignoran: el id lo genera la base, y "factura" (el lado dueno de la
    // relacion) se asigna a mano en FacturaServiceImpl.crear() -- ahi es donde se sabe
    // cual es la Factura padre, no aqui.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "factura", ignore = true)
    FacturaDetalleEty toEntity(FacturaDetalleDto dto);
}
