package mx.uumbal.solutions.palm_flow.modules.productores.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.EtapaPredio;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 200)
    private String nombre;

    @NotNull(message = "El predio es requerido")
    private UUID predioUuid;

    private Integer anioPlantacion;
    private EtapaPredio etapa;

    @Size(max = 50)
    private String idGis;

    private BigDecimal latitud;
    private BigDecimal longitud;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal hectareas;

    private boolean ramsar;
    private boolean anp;
    private boolean cambio;

    @Min(0) @Max(2)
    private Integer eudr = 0;

    @Min(0) @Max(4)
    private Integer riesgo = 0;

    private String wkt;
}
