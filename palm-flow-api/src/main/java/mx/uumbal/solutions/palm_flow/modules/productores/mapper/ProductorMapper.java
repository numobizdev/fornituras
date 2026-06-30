package mx.uumbal.solutions.palm_flow.modules.productores.mapper;

import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;

public interface ProductorMapper {

    Productor toEntity(ProductorRequestDTO dto);

    ProductorResponseDTO toResponseDTO(Productor entity);

    void applyRequest(Productor entity, ProductorRequestDTO dto);
}
