package org.example.mapper;

import org.example.dto.SesionCajaDto;
import org.example.model.SesionCajaEty;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.CDI)
public interface SesionCajaMapper {
    SesionCajaEty toEntity(SesionCajaDto sesionCajaDto);

    SesionCajaDto toDto(SesionCajaEty sesionCajaEty);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SesionCajaEty partialUpdate(SesionCajaDto sesionCajaDto, @MappingTarget SesionCajaEty sesionCajaEty);
}