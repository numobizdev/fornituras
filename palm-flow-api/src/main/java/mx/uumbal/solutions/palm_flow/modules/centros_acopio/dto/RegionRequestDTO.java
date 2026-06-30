package mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 150)
    private String nombre;
}
