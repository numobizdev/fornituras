package mx.uumbal.solutions.palm_flow.modules.productores.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.ActividadPrimaria;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.TipoPredio;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredioRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 200)
    private String nombre;

    @Size(max = 50)
    private String fiscalId;

    @NotNull(message = "El productor es requerido")
    private UUID productorUuid;

    @NotNull(message = "El centro de acopio es requerido")
    private UUID centroAcopioUuid;

    private Long estadoId;
    private Long municipioId;
    private Long comunidadId;

    private TipoPredio tipoPredio;
    private ActividadPrimaria actividadPrimaria;
}
