package mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper;

import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;

public interface CentroAcopioMapper {

    CentroAcopioResponseDTO toResponseDTO(CentroAcopio entity);

    void applyRequest(CentroAcopio entity, CentroAcopioRequestDTO dto);
}
