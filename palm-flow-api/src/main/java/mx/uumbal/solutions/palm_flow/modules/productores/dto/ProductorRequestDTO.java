package mx.uumbal.solutions.palm_flow.modules.productores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Frecuente;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Genero;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.NivelRspo;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.TipoPersona;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductorRequestDTO {

    @NotBlank(message = "El nombre es requerido")
    @Size(max = 200)
    private String nombre;

    @Size(max = 200)
    private String nombre2;

    private Genero genero;

    @Size(max = 20)
    private String telefono;

    @Email(message = "Correo electrónico inválido")
    @Size(max = 150)
    private String correoElectronico;

    @Size(max = 13)
    private String rfc;

    private TipoPersona tipoPersona;

    private boolean activo = true;

    private Frecuente frecuente;

    @Size(max = 50)
    private String typeCert;

    @Size(max = 50)
    private String idAkk;

    private NivelRspo nivelRspo;
}
