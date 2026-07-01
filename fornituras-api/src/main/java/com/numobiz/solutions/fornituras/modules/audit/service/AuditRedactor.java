package com.numobiz.solutions.fornituras.modules.audit.service;

import java.util.regex.Pattern;

/**
 * Redacta PII/secretos del detalle de auditoría antes de persistirlo (Principio V, FR-002). Es una
 * defensa en profundidad: las features ya registran por id y con nombres de campo (no valores), pero
 * si algún valor sensible se colara (CURP, RFC), aquí se enmascara. Nunca lanza: ante cualquier
 * entrada devuelve una cadena segura y acotada en longitud.
 */
public final class AuditRedactor {

	private static final String MASK = "***";
	private static final int MAX_LENGTH = 1000;

	// CURP: 4 letras + 6 dígitos + 6 letras + 2 alfanum. RFC: 3-4 letras + 6 dígitos + 3 alfanum.
	private static final Pattern CURP = Pattern.compile("\\b[A-Z]{4}\\d{6}[A-Z]{6}[A-Z0-9]{2}\\b");
	private static final Pattern RFC = Pattern.compile("\\b[A-Z&Ñ]{3,4}\\d{6}[A-Z0-9]{3}\\b");

	private AuditRedactor() {
	}

	public static String redact(String detail) {
		if (detail == null || detail.isBlank()) {
			return null;
		}
		String redacted = CURP.matcher(detail).replaceAll(MASK);
		redacted = RFC.matcher(redacted).replaceAll(MASK);
		if (redacted.length() > MAX_LENGTH) {
			redacted = redacted.substring(0, MAX_LENGTH);
		}
		return redacted;
	}
}
