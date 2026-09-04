package org.example.mapper;

import org.example.dto.FacturaDto;
import org.example.model.FacturaEty;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

// uses = FacturaDetalleMapper: para el campo "detalles" (List<FacturaDetalleEty> <->
// List<FacturaDetalleDto>), MapStruct aplica automaticamente FacturaDetalleMapper.toDto()/
// toEntity() a cada elemento de la lista.
@Mapper(uses = FacturaDetalleMapper.class)
public interface FacturaMapper {

    FacturaMapper INSTANCE = Mappers.getMapper(FacturaMapper.class);

    FacturaDto toDto(FacturaEty factura);

    // id se ignora: lo genera la base. sesionCaja no esta en FacturaDto -- no hay de donde
    // mapearlo, queda null (se asignaria aparte si hiciera falta esa relacion via API).
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sesionCaja", ignore = true)
    FacturaEty toEntity(FacturaDto dto);
}
