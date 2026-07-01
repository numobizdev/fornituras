package com.numobiz.solutions.fornituras.modules.landing.mapper;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionAdmin;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionPublic;
import com.numobiz.solutions.fornituras.modules.landing.dto.QuickLinkItem;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSection;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Traduce entre {@link LandingSection} y sus proyecciones, serializando los accesos rápidos
 * ({@link QuickLinkItem}) a/desde el JSON de {@code config_json}. La proyección pública excluye por
 * diseño id, scope, estado y timestamps (cero PII, ADR 0015).
 */
@Component
public class LandingSectionMapper {

	private final ObjectMapper objectMapper;

	public LandingSectionMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public LandingSectionPublic toPublic(LandingSection section) {
		return new LandingSectionPublic(
				section.getType(),
				section.getTitulo(),
				section.getSubtitulo(),
				section.getCuerpo(),
				section.getImagenUrl(),
				section.getCtaLabel(),
				section.getCtaUrl(),
				section.getOrden(),
				readQuickLinks(section.getConfigJson()));
	}

	public LandingSectionAdmin toAdmin(LandingSection section) {
		return new LandingSectionAdmin(
				section.getId(),
				section.getScope(),
				section.getType(),
				section.getTitulo(),
				section.getSubtitulo(),
				section.getCuerpo(),
				section.getImagenUrl(),
				section.getCtaLabel(),
				section.getCtaUrl(),
				section.getOrden(),
				section.isActive(),
				readQuickLinks(section.getConfigJson()),
				section.getCreatedAt(),
				section.getUpdatedAt());
	}

	/** Serializa la lista de accesos a JSON, o {@code null} si no hay accesos (tipos distintos de QUICK_LINKS). */
	public String writeQuickLinks(List<QuickLinkItem> quickLinks) {
		if (quickLinks == null || quickLinks.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(quickLinks);
		} catch (JacksonException ex) {
			throw new BadRequestException("Los accesos rápidos no son válidos.");
		}
	}

	private List<QuickLinkItem> readQuickLinks(String configJson) {
		if (configJson == null || configJson.isBlank()) {
			return List.of();
		}
		try {
			return List.of(objectMapper.readValue(configJson, QuickLinkItem[].class));
		} catch (JacksonException ex) {
			// Contenido corrupto en BD: se degrada a lista vacía en lugar de romper el render.
			return List.of();
		}
	}
}
