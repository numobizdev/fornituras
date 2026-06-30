package mx.uumbal.solutions.palm_flow.modules.centros_acopio.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CentroAcopioPrecioKgRequestDTO {

    @NotNull(message = "El precio por kg es requerido")
    @DecimalMin(value = "0.01", message = "El precio por kg debe ser mayor a cero")
    private BigDecimal precioKg;

    @NotNull(message = "La fecha de vigencia es requerida")
    private LocalDate fechaVigencia;
}
