package mx.uumbal.solutions.palm_flow.modules.geografia.mapper;

import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.dto.MunicipioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;

public interface MunicipioMapper {

    MunicipioResponseDTO toResponseDTO(Municipio entity);

    void updateEntity(Municipio entity, MunicipioRequestDTO dto);
}
