package mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper;

import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioPrecioKgResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopioPrecioKg;
import org.springframework.stereotype.Component;

@Component
public class CentroAcopioPrecioKgMapperImpl implements CentroAcopioPrecioKgMapper {

    @Override
    public CentroAcopioPrecioKgResponseDTO toResponseDTO(CentroAcopioPrecioKg entity) {
        if (entity == null) {
            return null;
        }
        return CentroAcopioPrecioKgResponseDTO.builder()
                .uuid(entity.getUuid())
                .centroAcopioUuid(entity.getCentroAcopio().getUuid())
                .precioKg(entity.getPrecioKg())
                .fechaVigencia(entity.getFechaVigencia())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdByUserId(entity.getCreatedByUser() != null ? entity.getCreatedByUser().getId() : null)
                .createdByUserEmail(entity.getCreatedByUser() != null ? entity.getCreatedByUser().getEmail() : null)
                .updatedByUserId(entity.getUpdatedByUser() != null ? entity.getUpdatedByUser().getId() : null)
                .updatedByUserEmail(entity.getUpdatedByUser() != null ? entity.getUpdatedByUser().getEmail() : null)
                .build();
    }
}
