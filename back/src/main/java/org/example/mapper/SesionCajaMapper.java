package org.example.mapper;

import org.example.dto.SesionCajaDTO;
import org.example.model.SesionCaja;

import java.util.stream.Collectors;

public final class SesionCajaMapper {

    private SesionCajaMapper() {
    }

    public static SesionCajaDTO toDTO(SesionCaja sesion) {
        if (sesion == null) {
            return null;
        }
        SesionCajaDTO dto = new SesionCajaDTO();
        dto.setId(sesion.getId());
        dto.setFApertura(sesion.getFApertura());
        dto.setFCierre(sesion.getFCierre());
        dto.setCerrada(sesion.isCerrada());          // boolean primitivo -> isCerrada(), no getCerrada()
        dto.setMontoApertura(sesion.getMontoApertura());
        dto.setMontoCierre(sesion.getMontoCierre());
        dto.setCajero(sesion.getCajero());
        dto.setLocacion(sesion.getLocacion());
        dto.setFacturas(sesion.getFacturas().stream()
                .map(FacturaMapper::toDTO)            // reusa el Mapper que ya existe
                .collect(Collectors.toList()));
        return dto;
    }

    public static SesionCaja toEntity(SesionCajaDTO dto) {
        SesionCaja sesion = new SesionCaja();
        sesion.setCajero(dto.getCajero());
        sesion.setLocacion(dto.getLocacion());
        sesion.setMontoApertura(dto.getMontoApertura());
        // fApertura, cerrada y facturas NO se copian del dto al abrir -- los decide el
        // servidor (Paso 3), igual que FacturaServiceImpl decide "fecha" si no viene.
        return sesion;
    }
}