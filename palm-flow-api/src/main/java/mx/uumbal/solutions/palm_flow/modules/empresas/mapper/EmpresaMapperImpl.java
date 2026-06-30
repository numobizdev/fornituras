package mx.uumbal.solutions.palm_flow.modules.empresas.mapper;

import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.entity.Empresa;
import org.springframework.stereotype.Component;

@Component
public class EmpresaMapperImpl implements EmpresaMapper {

    @Override
    public Empresa toEntity(EmpresaRequestDTO dto) {
        if (dto == null) return null;
        return Empresa.builder()
                .nombre(dto.getNombre())
                .slug(dto.getSlug())
                .activo(dto.isActivo())
                .build();
    }

    @Override
    public EmpresaResponseDTO toResponseDTO(Empresa empresa) {
        if (empresa == null) return null;
        return EmpresaResponseDTO.builder()
                .id(empresa.getId())
                .nombre(empresa.getNombre())
                .slug(empresa.getSlug())
                .activo(empresa.isActivo())
                .createdAt(empresa.getCreatedAt())
                .updatedAt(empresa.getUpdatedAt())
                .createdBy(empresa.getCreatedBy())
                .updatedBy(empresa.getUpdatedBy())
                .build();
    }
}
