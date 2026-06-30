package mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper;

import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.RegionResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.Region;

public interface RegionMapper {

    Region toEntity(RegionRequestDTO dto);

    RegionResponseDTO toResponseDTO(Region entity);
}
