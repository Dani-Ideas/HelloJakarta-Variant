package org.example.mapper;

import org.example.dto.FacturaDetalleDto;
import org.example.model.FacturaDetalleEty;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.CDI, uses = {FacturaMapper.class, ProductoMapper.class})
public interface FacturaDetalleMapper {
    FacturaDetalleEty toEntity(FacturaDetalleDto facturaDetalleDto);

    FacturaDetalleDto toDto(FacturaDetalleEty facturaDetalleEty);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FacturaDetalleEty partialUpdate(FacturaDetalleDto facturaDetalleDto, @MappingTarget FacturaDetalleEty facturaDetalleEty);
}