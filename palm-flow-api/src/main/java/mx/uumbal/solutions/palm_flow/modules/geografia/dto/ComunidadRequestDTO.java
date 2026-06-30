package mx.uumbal.solutions.palm_flow.modules.geografia.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComunidadRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 150)
    private String nombre;

    @NotNull(message = "El municipio es requerido")
    private Long municipioId;
}
