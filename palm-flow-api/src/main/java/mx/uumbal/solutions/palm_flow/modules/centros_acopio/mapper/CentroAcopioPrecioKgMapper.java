package mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper;

import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioPrecioKgResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopioPrecioKg;

public interface CentroAcopioPrecioKgMapper {

    CentroAcopioPrecioKgResponseDTO toResponseDTO(CentroAcopioPrecioKg entity);
}
