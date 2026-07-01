package com.numobiz.solutions.fornituras.modules.landing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Acceso rápido de una sección {@code QUICK_LINKS}: etiqueta visible, destino y (opcional) icono
 * Ionicons. La etiqueta es texto plano; la URL se valida con {@link SafeUrl} (ruta interna o http/https).
 */
public record QuickLinkItem(
		@NotBlank(message = "La etiqueta del acceso es obligatoria")
		@Size(max = 60, message = "La etiqueta no debe exceder 60 caracteres")
		String label,

		@NotBlank(message = "El destino del acceso es obligatorio")
		@Size(max = 512, message = "El destino no debe exceder 512 caracteres")
		@SafeUrl
		String url,

		@Size(max = 60, message = "El icono no debe exceder 60 caracteres")
		String icon
) {
}
