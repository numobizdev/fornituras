package com.numobiz.solutions.fornituras.modules.landing.dto;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Proyección completa de una sección para el editor de ADMIN: incluye id, scope, estado y timestamps
 * además del contenido. Se sirve solo a través de los endpoints restringidos a ADMIN.
 */
public record LandingSectionAdmin(
		Long id,
		LandingScope scope,
		LandingSectionType type,
		String titulo,
		String subtitulo,
		String cuerpo,
		String imagenUrl,
		String ctaLabel,
		String ctaUrl,
		int orden,
		boolean active,
		List<QuickLinkItem> quickLinks,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
