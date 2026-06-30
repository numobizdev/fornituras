package mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopioActivo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroAcopioResponseDTO {

    private UUID uuid;
    private String nombre;
    private Long regionId;
    private String regionNombre;
    private CentroAcopioActivo activo;
    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal latitud;
    private BigDecimal longitud;
    private String encargado;
    private Long estadoId;
    private String estadoNombre;
    private Long municipioId;
    private String municipioNombre;
    private Long comunidadId;
    private String comunidadNombre;
    private BigDecimal distanciaKm;
    private String alias;
    private String direccion;
    private String correo;
    private String rfc;
    private String telefono;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdByUserId;
    private String createdByUserEmail;
    private Long updatedByUserId;
    private String updatedByUserEmail;
}
