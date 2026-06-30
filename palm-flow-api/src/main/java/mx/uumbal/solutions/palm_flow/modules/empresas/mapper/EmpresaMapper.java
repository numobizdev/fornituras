package mx.uumbal.solutions.palm_flow.modules.empresas.mapper;

import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaRequestDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.dto.EmpresaResponseDTO;
import mx.uumbal.solutions.palm_flow.modules.empresas.entity.Empresa;

public interface EmpresaMapper {

    Empresa toEntity(EmpresaRequestDTO dto);

    EmpresaResponseDTO toResponseDTO(Empresa empresa);
}
