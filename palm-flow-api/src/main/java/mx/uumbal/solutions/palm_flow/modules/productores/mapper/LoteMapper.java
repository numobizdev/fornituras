package mx.uumbal.solutions.palm_flow.modules.productores.mapper;

import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteSummaryDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Lote;

public interface LoteMapper {

    LoteResponseDTO toResponseDTO(Lote entity);

    LoteSummaryDTO toSummaryDTO(Lote entity);

    void applyRequest(Lote entity, LoteRequestDTO dto);
}
