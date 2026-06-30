package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.mapper;

import java.math.BigDecimal;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto.RecepcionFrutaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFruta;

public interface RecepcionFrutaMapper {

    RecepcionFrutaResponseDTO toResponseDTO(RecepcionFruta entity);

    void applyRequest(RecepcionFruta entity, RecepcionFrutaRequestDTO dto);

    void applyPricing(RecepcionFruta entity, BigDecimal precioKg);
}
