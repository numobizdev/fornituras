package mx.uumbal.solutions.palm_flow.modules.empresas.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResponseDTO {

    private Long id;
    private String nombre;
    private String slug;
    private boolean activo;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
