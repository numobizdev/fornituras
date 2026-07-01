package com.numobiz.solutions.fornituras.modules.landing.dto;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;

import java.util.List;

/**
 * Proyección de solo lectura de una sección para las caras pública ({@code /public}) y de inicio
 * ({@code /home}). Expone únicamente lo necesario para renderizar: <b>sin</b> id interno, scope,
 * banderas administrativas ni timestamps, y <b>sin PII</b> por diseño (FR-006, SC-002).
 */
public record LandingSectionPublic(
		LandingSectionType type,
		String titulo,
		String subtitulo,
		String cuerpo,
		String imagenUrl,
		String ctaLabel,
		String ctaUrl,
		int orden,
		List<QuickLinkItem> quickLinks
) {
}
