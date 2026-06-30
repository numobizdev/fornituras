package mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopioActivo;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroAcopioRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 200)
    private String nombre;

    @NotNull(message = "La región es requerida")
    private Long regionId;

    @NotNull(message = "El estado activo es requerido")
    private CentroAcopioActivo activo;

    private BigDecimal x;
    private BigDecimal y;
    private BigDecimal latitud;
    private BigDecimal longitud;

    @Size(max = 200)
    private String encargado;

    @NotNull(message = "El estado geográfico es requerido")
    private Long estadoId;

    @NotNull(message = "El municipio es requerido")
    private Long municipioId;

    @NotNull(message = "La comunidad es requerida")
    private Long comunidadId;

    private BigDecimal distanciaKm;

    @Size(max = 100)
    private String alias;

    @Size(max = 300)
    private String direccion;

    @Size(max = 80)
    private String correo;

    @Size(max = 13)
    private String rfc;

    @Size(max = 10)
    private String telefono;
}
