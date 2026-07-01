package com.numobiz.solutions.fornituras.modules.landing.service;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.modules.landing.dto.QuickLinkItem;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;

import java.util.List;

/**
 * Reglas estructurales de una sección que dependen del tipo y no se expresan bien con anotaciones de
 * campo. Se aplican en el servicio (borde de negocio) y lanzan {@link BadRequestException} → HTTP 400.
 */
final class LandingSectionRules {

	private LandingSectionRules() {
	}

	static void validate(
			LandingSectionType type,
			String titulo,
			String ctaLabel,
			String ctaUrl,
			List<QuickLinkItem> quickLinks) {

		boolean tituloRequerido = type == LandingSectionType.HERO
				|| type == LandingSectionType.ANNOUNCEMENT
				|| type == LandingSectionType.RICH_TEXT;
		if (tituloRequerido && isBlank(titulo)) {
			throw new BadRequestException("El título es obligatorio para el tipo " + type + ".");
		}

		if (type == LandingSectionType.QUICK_LINKS) {
			if (quickLinks == null || quickLinks.isEmpty()) {
				throw new BadRequestException("Una sección de accesos rápidos requiere al menos un acceso.");
			}
		} else if (quickLinks != null && !quickLinks.isEmpty()) {
			throw new BadRequestException("Solo las secciones de accesos rápidos admiten accesos.");
		}

		boolean hasLabel = !isBlank(ctaLabel);
		boolean hasUrl = !isBlank(ctaUrl);
		if (hasLabel != hasUrl) {
			throw new BadRequestException("La etiqueta y el destino del botón de acción van juntos.");
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
