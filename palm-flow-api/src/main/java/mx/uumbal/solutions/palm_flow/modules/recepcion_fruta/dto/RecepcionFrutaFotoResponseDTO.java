package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.TipoFotoRecepcion;

@Value
@Builder
public class RecepcionFrutaFotoResponseDTO {
    UUID uuid;
    UUID recepcionFrutaUuid;
    TipoFotoRecepcion tipo;
    String contentType;
    Instant createdAt;
}
