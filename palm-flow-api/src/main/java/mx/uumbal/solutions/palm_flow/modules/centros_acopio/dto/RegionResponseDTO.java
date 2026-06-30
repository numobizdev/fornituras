package mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionResponseDTO {

    private Long id;
    private String nombre;
}
