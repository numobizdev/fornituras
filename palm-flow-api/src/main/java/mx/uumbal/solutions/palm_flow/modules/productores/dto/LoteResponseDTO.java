package mx.uumbal.solutions.palm_flow.modules.productores.dto;

import java.math.BigDecimal;
import java.time.Instant;
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
public class LoteResponseDTO {

    private UUID uuid;
    private String nombre;
    private UUID predioUuid;
    private String predioNombre;
    private UUID productorUuid;
    private String productorNombre;
    private UUID centroAcopioUuid;
    private String centroAcopioNombre;
    private Integer anioPlantacion;
    private EtapaPredio etapa;
    private String idGis;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal hectareas;
    private boolean ramsar;
    private boolean anp;
    private boolean cambio;
    private Integer eudr;
    private Integer riesgo;
    private String wkt;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdByUserId;
    private String createdByUserEmail;
    private Long updatedByUserId;
    private String updatedByUserEmail;
}
