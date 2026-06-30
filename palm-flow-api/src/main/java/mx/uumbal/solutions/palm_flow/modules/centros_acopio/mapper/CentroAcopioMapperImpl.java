package mx.uumbal.solutions.palm_flow.modules.centros_acopio.mapper;

import mx.uumbal.solutions.palm_flow.common.audit.UserAuditMapper;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto.CentroAcopioResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import org.springframework.stereotype.Component;

@Component
public class CentroAcopioMapperImpl implements CentroAcopioMapper {

    @Override
    public CentroAcopioResponseDTO toResponseDTO(CentroAcopio entity) {
        if (entity == null) {
            return null;
        }
        UserAuditMapper.Fields audit = UserAuditMapper.toFields(entity);
        return CentroAcopioResponseDTO.builder()
                .uuid(entity.getUuid())
                .nombre(entity.getNombre())
                .regionId(entity.getRegion().getId())
                .regionNombre(entity.getRegion().getNombre())
                .activo(entity.getActivo())
                .x(entity.getX())
                .y(entity.getY())
                .latitud(entity.getLatitud())
                .longitud(entity.getLongitud())
                .encargado(entity.getEncargado())
                .estadoId(entity.getEstado().getId())
                .estadoNombre(entity.getEstado().getNombre())
                .municipioId(entity.getMunicipio().getId())
                .municipioNombre(entity.getMunicipio().getNombre())
                .comunidadId(entity.getComunidad().getId())
                .comunidadNombre(entity.getComunidad().getNombre())
                .distanciaKm(entity.getDistanciaKm())
                .alias(entity.getAlias())
                .direccion(entity.getDireccion())
                .correo(entity.getCorreo())
                .rfc(entity.getRfc())
                .telefono(entity.getTelefono())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .createdByUserId(audit.getCreatedByUserId())
                .createdByUserEmail(audit.getCreatedByUserEmail())
                .updatedByUserId(audit.getUpdatedByUserId())
                .updatedByUserEmail(audit.getUpdatedByUserEmail())
                .build();
    }

    @Override
    public void applyRequest(CentroAcopio entity, CentroAcopioRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setNombre(dto.getNombre());
        entity.setActivo(dto.getActivo());
        entity.setX(dto.getX());
        entity.setY(dto.getY());
        entity.setLatitud(dto.getLatitud());
        entity.setLongitud(dto.getLongitud());
        entity.setEncargado(dto.getEncargado());
        entity.setDistanciaKm(dto.getDistanciaKm());
        entity.setAlias(dto.getAlias());
        entity.setDireccion(dto.getDireccion());
        entity.setCorreo(dto.getCorreo());
        entity.setRfc(dto.getRfc());
        entity.setTelefono(dto.getTelefono());
    }
}
