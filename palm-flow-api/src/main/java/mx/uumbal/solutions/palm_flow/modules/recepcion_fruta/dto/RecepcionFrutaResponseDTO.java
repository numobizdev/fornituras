package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.CalidadFruta;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.OrigenPeso;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecepcionFrutaResponseDTO {

    private UUID uuid;
    private String folio;
    private Instant fecha;
    private UUID centroAcopioUuid;
    private String centroAcopioNombre;
    private UUID productorUuid;
    private String productorNombre;
    private UUID predioUuid;
    private String predioNombre;
    private UUID loteUuid;
    private String loteNombre;
    private String placa;
    private String modelo;
    private String marca;
    private String tipoColor;
    private String propietario;
    private BigDecimal pesoBruto;
    private BigDecimal pesoTara;
    private BigDecimal pesoNeto;
    private BigDecimal precioKg;
    private BigDecimal montoAPagar;
    private OrigenPeso origenPeso;
    private List<CalidadFruta> calidadFruta;
    private Long usuarioId;
    private String usuarioEmail;
    private boolean registroOffline;
    private Instant fechaSync;
    private Instant createdAt;
    private Instant updatedAt;
}
