package mx.uumbal.solutions.palm_flow.modules.productores.mapper;

import mx.uumbal.solutions.palm_flow.common.audit.UserAuditMapper;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.PredioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;
import mx.uumbal.solutions.palm_flow.modules.productores.util.FiscalIdNormalizer;
import org.springframework.stereotype.Component;

@Component
public class PredioMapperImpl implements PredioMapper {

    @Override
    public PredioResponseDTO toResponseDTO(Predio entity) {
        if (entity == null) {
            return null;
        }
        UserAuditMapper.Fields audit = UserAuditMapper.toFields(entity);
        return PredioResponseDTO.builder()
                .uuid(entity.getUuid())
                .nombre(entity.getNombre())
                .fiscalId(entity.getFiscalId())
                .productorUuid(entity.getProductor().getUuid())
                .productorNombre(entity.getProductor().getNombre())
                .centroAcopioUuid(entity.getCentroAcopio().getUuid())
                .centroAcopioNombre(entity.getCentroAcopio().getNombre())
                .estadoId(entity.getEstado() != null ? entity.getEstado().getId() : null)
                .estadoNombre(entity.getEstado() != null ? entity.getEstado().getNombre() : null)
                .municipioId(entity.getMunicipio() != null ? entity.getMunicipio().getId() : null)
                .municipioNombre(entity.getMunicipio() != null ? entity.getMunicipio().getNombre() : null)
                .comunidadId(entity.getComunidad() != null ? entity.getComunidad().getId() : null)
                .comunidadNombre(entity.getComunidad() != null ? entity.getComunidad().getNombre() : null)
                .tipoPredio(entity.getTipoPredio())
                .actividadPrimaria(entity.getActividadPrimaria())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .createdByUserId(audit.getCreatedByUserId())
                .createdByUserEmail(audit.getCreatedByUserEmail())
                .updatedByUserId(audit.getUpdatedByUserId())
                .updatedByUserEmail(audit.getUpdatedByUserEmail())
                .build();
    }

    @Override
    public void applyRequest(Predio entity, PredioRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setNombre(dto.getNombre());
        entity.setFiscalId(FiscalIdNormalizer.normalize(dto.getFiscalId()));
        entity.setTipoPredio(dto.getTipoPredio());
        entity.setActividadPrimaria(dto.getActividadPrimaria());
    }
}
