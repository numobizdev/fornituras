package com.numobiz.solutions.fornituras.modules.landing.entity;

/**
 * Tipo de sección de contenido. Determina qué campos son relevantes y cómo se renderiza:
 * {@code HERO} (encabezado con título/subtítulo/imagen/CTA), {@code ANNOUNCEMENT} (aviso),
 * {@code QUICK_LINKS} (lista de accesos rápidos en {@code config_json}) y {@code RICH_TEXT}
 * (bloque de texto). El contenido es siempre texto plano; el escape ocurre en el render (ADR 0015).
 */
public enum LandingSectionType {
	HERO,
	ANNOUNCEMENT,
	QUICK_LINKS,
	RICH_TEXT
}
