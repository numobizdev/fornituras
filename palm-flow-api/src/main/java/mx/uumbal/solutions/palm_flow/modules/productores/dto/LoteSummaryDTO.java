package mx.uumbal.solutions.palm_flow.modules.productores.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteSummaryDTO {

    private UUID uuid;
    private String nombre;
    private String idGis;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String wkt;
}
