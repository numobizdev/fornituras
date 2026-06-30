package mx.uumbal.solutions.palm_flow.modules.productores.dto;

import java.time.Instant;
import java.util.List;
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
public class PredioResponseDTO {

    private UUID uuid;
    private String nombre;
    private String fiscalId;
    private UUID productorUuid;
    private String productorNombre;
    private UUID centroAcopioUuid;
    private String centroAcopioNombre;
    private Long estadoId;
    private String estadoNombre;
    private Long municipioId;
    private String municipioNombre;
    private Long comunidadId;
    private String comunidadNombre;
    private TipoPredio tipoPredio;
    private ActividadPrimaria actividadPrimaria;
    private List<LoteSummaryDTO> lotes;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdByUserId;
    private String createdByUserEmail;
    private Long updatedByUserId;
    private String updatedByUserEmail;
}
