package org.example.mapper;

import org.example.dto.FacturaDto;
import org.example.model.FacturaEty;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.CDI, uses = {SesionCajaMapper.class})
public interface FacturaMapper {
    FacturaEty toEntity(FacturaDto facturaDto);

    FacturaDto toDto(FacturaEty facturaEty);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    FacturaEty partialUpdate(FacturaDto facturaDto, @MappingTarget FacturaEty facturaEty);
}