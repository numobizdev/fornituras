package com.numobiz.solutions.fornituras.modules.landing.dto;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Alta de una sección de contenido (solo ADMIN). Las longitudes y los esquemas de URL se validan aquí
 * (en el borde); las reglas estructurales dependientes del tipo (título obligatorio para
 * HERO/ANNOUNCEMENT/RICH_TEXT, accesos obligatorios para QUICK_LINKS, CTA en pareja) las verifica el
 * servicio con {@link LandingSectionRules}. El texto se almacena literal y se escapa en el render.
 */
public record LandingSectionCreateRequest(
		@NotNull(message = "La cara (scope) es obligatoria")
		LandingScope scope,

		@NotNull(message = "El tipo de sección es obligatorio")
		LandingSectionType type,

		@Size(max = 160, message = "El título no debe exceder 160 caracteres")
		String titulo,

		@Size(max = 240, message = "El subtítulo no debe exceder 240 caracteres")
		String subtitulo,

		@Size(max = 2000, message = "El cuerpo no debe exceder 2000 caracteres")
		String cuerpo,

		@Size(max = 512, message = "La URL de imagen no debe exceder 512 caracteres")
		@SafeUrl
		String imagenUrl,

		@Size(max = 80, message = "La etiqueta del CTA no debe exceder 80 caracteres")
		String ctaLabel,

		@Size(max = 512, message = "La URL del CTA no debe exceder 512 caracteres")
		@SafeUrl
		String ctaUrl,

		int orden,

		List<@Valid QuickLinkItem> quickLinks
) {
}
