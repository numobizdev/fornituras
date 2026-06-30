package mx.uumbal.solutions.palm_flow.modules.productores.mapper;

import mx.uumbal.solutions.palm_flow.common.audit.UserAuditMapper;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.dto.ProductorResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;
import org.springframework.stereotype.Component;

@Component
public class ProductorMapperImpl implements ProductorMapper {

    @Override
    public Productor toEntity(ProductorRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        Productor productor = new Productor();
        applyRequest(productor, dto);
        return productor;
    }

    @Override
    public ProductorResponseDTO toResponseDTO(Productor entity) {
        if (entity == null) {
            return null;
        }
        UserAuditMapper.Fields audit = UserAuditMapper.toFields(entity);
        return ProductorResponseDTO.builder()
                .uuid(entity.getUuid())
                .nombre(entity.getNombre())
                .nombre2(entity.getNombre2())
                .genero(entity.getGenero())
                .telefono(entity.getTelefono())
                .correoElectronico(entity.getCorreoElectronico())
                .rfc(entity.getRfc())
                .tipoPersona(entity.getTipoPersona())
                .activo(entity.isActivo())
                .frecuente(entity.getFrecuente())
                .typeCert(entity.getTypeCert())
                .idAkk(entity.getIdAkk())
                .nivelRspo(entity.getNivelRspo())
                .createdAt(audit.getCreatedAt())
                .updatedAt(audit.getUpdatedAt())
                .createdByUserId(audit.getCreatedByUserId())
                .createdByUserEmail(audit.getCreatedByUserEmail())
                .updatedByUserId(audit.getUpdatedByUserId())
                .updatedByUserEmail(audit.getUpdatedByUserEmail())
                .build();
    }

    @Override
    public void applyRequest(Productor entity, ProductorRequestDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setNombre(dto.getNombre());
        entity.setNombre2(dto.getNombre2());
        entity.setGenero(dto.getGenero());
        entity.setTelefono(dto.getTelefono());
        entity.setCorreoElectronico(dto.getCorreoElectronico());
        entity.setRfc(dto.getRfc());
        entity.setTipoPersona(dto.getTipoPersona());
        entity.setActivo(dto.isActivo());
        entity.setFrecuente(dto.getFrecuente());
        entity.setTypeCert(dto.getTypeCert());
        entity.setIdAkk(dto.getIdAkk());
        entity.setNivelRspo(dto.getNivelRspo());
    }
}
