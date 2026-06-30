package mx.uumbal.solutions.palm_flow.modules.productores.mapper;

import mx.uumbal.solutions.palm_flow.common.audit.UserAuditMapper;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.LoteSummaryDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Lote;
import org.springframework.stereotype.Component;

@Component
public class LoteMapperImpl implements LoteMapper {

    @Override
    public LoteResponseDTO toResponseDTO(Lote entity) {
        if (entity == null) {
            return null;
        }
        UserAuditMapper.Fields audit = UserAuditMapper.toFields(entity);
        return LoteResponseDTO.builder()
                .uuid(entity.getUuid())
                .nombre(entity.getNombre())
                .predioUuid(entity.getPredio().getUuid())
                .predioNombre(entity.getPredio().getNombre())
                .productorUuid(entity.getPredio().getProductor().getUuid())
                .productorNombre(entity.getPredio().getProductor().getNombre())
                .centroAcopioUuid(entity.getPredio().getCentroAcopio().getUuid())
                .centroAcopioNombre(entity.getPredio().getCentroAcopio().getNombre())
                .anioPlantacion(entity.getAnioPlantacion())
                .etapa(entity.getEtapa())
                .idGis(entity.getIdGis())
                .latitud(entity.getLatitud())
                .longitud(entity.getLongitud())
                .x(entity.getX())
                .y(entity.getY())
                .hectareas(entity.getHectareas())
                .ramsar(entity.isRamsar())
                .anp(entity.isAnp())
                .cambio(entity.isCambio())
                .eudr(entity.getEudr())
                .riesgo(entity.getRiesgo())
                .wkt(entity.getWkt())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .createdByUserId(audit.getCreatedByUserId())
                .createdByUserEmail(audit.getCreatedByUserEmail())
                .updatedByUserId(audit.getUpdatedByUserId())
                .updatedByUserEmail(audit.getUpdatedByUserEmail())
                .build();
    }

    @Override
    public LoteSummaryDTO toSummaryDTO(Lote entity) {
        if (entity == null) {
            return null;
        }
        return LoteSummaryDTO.builder()
                .uuid(entity.getUuid())
                .nombre(entity.getNombre())
                .idGis(entity.getIdGis())
                .latitud(entity.getLatitud())
                .longitud(entity.getLongitud())
                .wkt(entity.getWkt())
                .build();
    }

    @Override
    public void applyRequest(Lote entity, LoteRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setNombre(dto.getNombre());
        entity.setAnioPlantacion(dto.getAnioPlantacion());
        entity.setEtapa(dto.getEtapa());
        entity.setIdGis(dto.getIdGis());
        entity.setLatitud(dto.getLatitud());
        entity.setLongitud(dto.getLongitud());
        entity.setX(dto.getX());
        entity.setY(dto.getY());
        entity.setHectareas(dto.getHectareas());
        entity.setRamsar(dto.isRamsar());
        entity.setAnp(dto.isAnp());
        entity.setCambio(dto.isCambio());
        entity.setEudr(dto.getEudr() != null ? dto.getEudr() : 0);
        entity.setRiesgo(dto.getRiesgo() != null ? dto.getRiesgo() : 0);
        entity.setWkt(dto.getWkt());
    }
}
