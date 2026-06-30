package mx.uumbal.solutions.palm_flow.modules.productores.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Frecuente;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Genero;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.NivelRspo;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.TipoPersona;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductorResponseDTO {

    private UUID uuid;
    private String nombre;
    private String nombre2;
    private Genero genero;
    private String telefono;
    private String correoElectronico;
    private String rfc;
    private TipoPersona tipoPersona;
    private boolean activo;
    private Frecuente frecuente;
    private String typeCert;
    private String idAkk;
    private NivelRspo nivelRspo;
    private Instant createdAt;
    private Instant updatedAt;
    private Long createdByUserId;
    private String createdByUserEmail;
    private Long updatedByUserId;
    private String updatedByUserEmail;
}
