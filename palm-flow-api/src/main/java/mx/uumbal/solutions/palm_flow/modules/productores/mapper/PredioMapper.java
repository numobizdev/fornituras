package mx.uumbal.solutions.palm_flow.modules.productores.mapper;

import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;

public interface PredioMapper {

    PredioResponseDTO toResponseDTO(Predio entity);

    void applyRequest(Predio entity, PredioRequestDTO dto);
}
