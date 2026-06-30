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
public class MunicipioRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 150)
    private String nombre;

    @NotNull(message = "El estado es requerido")
    private Long estadoId;
}
