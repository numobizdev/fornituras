package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class RecepcionFrutaRequestDTO {

    @NotNull(message = "La fecha es requerida")
    private Instant fecha;

    @NotNull(message = "El centro de acopio es requerido")
    private UUID centroAcopioUuid;

    @NotNull(message = "El productor es requerido")
    private UUID productorUuid;

    @NotNull(message = "El predio es requerido")
    private UUID predioUuid;

    private UUID loteUuid;

    @Size(max = 20)
    private String placa;

    @Size(max = 100)
    private String modelo;

    @Size(max = 100)
    private String marca;

    @Size(max = 50)
    private String tipoColor;

    @Size(max = 200)
    private String propietario;

    private BigDecimal pesoBruto;
    private BigDecimal pesoTara;
    private BigDecimal pesoNeto;
    private OrigenPeso origenPeso;

    @NotEmpty(message = "La calidad de la fruta es requerida")
    private List<CalidadFruta> calidadFruta;

    @NotNull(message = "El usuario es requerido")
    private Long usuarioId;

    @Builder.Default
    private Boolean registroOffline = false;

    private Instant fechaSync;
}
