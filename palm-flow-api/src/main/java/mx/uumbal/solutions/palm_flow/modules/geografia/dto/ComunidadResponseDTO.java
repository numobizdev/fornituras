package mx.uumbal.solutions.palm_flow.modules.geografia.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComunidadResponseDTO {

    private Long id;
    private String nombre;
    private Long municipioId;
    private String municipioNombre;
    private Long estadoId;
    private String estadoNombre;
}
