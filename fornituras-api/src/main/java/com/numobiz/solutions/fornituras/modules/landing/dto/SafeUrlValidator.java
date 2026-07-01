package com.numobiz.solutions.fornituras.modules.landing.dto;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

/**
 * Valida el esquema de una URL de contenido (imagen, CTA, acceso rápido) para evitar inyección de
 * esquemas ejecutables (ADR 0015). Permite valores vacíos (opcionales), rutas internas relativas y
 * {@code http}/{@code https}; rechaza {@code //host}, {@code javascript:}, {@code data:}, etc.
 */
public class SafeUrlValidator implements ConstraintValidator<SafeUrl, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null || value.isBlank()) {
			return true;
		}
		String trimmed = value.trim();
		if (trimmed.startsWith("//")) {
			// URL "protocol-relative": hereda esquema del contexto; se rechaza por prudencia.
			return false;
		}
		if (trimmed.startsWith("/")) {
			return true;
		}
		String lower = trimmed.toLowerCase(Locale.ROOT);
		return lower.startsWith("http://") || lower.startsWith("https://");
	}
}
