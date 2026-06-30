package com.numobiz.solutions.fornituras.modules.officers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Alta/edición de un elemento. La validación de borde cubre formato (placa, CURP/RFC si se
 * capturan); unicidad y blind index se resuelven en el servicio. CURP/RFC son opcionales mientras
 * el ADR 0003 no fije su captura obligatoria.
 */
public record OfficerCreateRequest(
		@NotBlank(message = "El nombre es obligatorio")
		@Size(max = 120, message = "El nombre no debe exceder 120 caracteres")
		String nombre,

		@NotBlank(message = "El apellido paterno es obligatorio")
		@Size(max = 120, message = "El apellido paterno no debe exceder 120 caracteres")
		String apellidoPaterno,

		@Size(max = 120, message = "El apellido materno no debe exceder 120 caracteres")
		String apellidoMaterno,

		@NotBlank(message = "La placa (identificador) es obligatoria")
		@Size(max = 40, message = "La placa no debe exceder 40 caracteres")
		String placa,

		@NotNull(message = "El sexo es obligatorio")
		Long sexoId,

		Long tipoSangreId,

		@NotNull(message = "El municipio es obligatorio")
		Long municipioId,

		@Pattern(regexp = "^[A-Za-z0-9]{18}$", message = "La CURP debe tener 18 caracteres alfanuméricos")
		String curp,

		@Pattern(regexp = "^[A-Za-z0-9]{12,13}$", message = "El RFC debe tener 12 o 13 caracteres alfanuméricos")
		String rfc,

		@Size(max = 500, message = "La URL de foto no debe exceder 500 caracteres")
		String fotoUrl
) {
}
