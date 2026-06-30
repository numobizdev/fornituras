package mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroAcopioPrecioKgResponseDTO {

    private UUID uuid;
    private UUID centroAcopioUuid;
    private BigDecimal precioKg;
    private LocalDate fechaVigencia;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdByUserId;
    private String createdByUserEmail;
    private Long updatedByUserId;
    private String updatedByUserEmail;
}
