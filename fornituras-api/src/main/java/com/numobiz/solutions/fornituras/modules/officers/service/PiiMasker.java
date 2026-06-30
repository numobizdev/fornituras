package com.numobiz.solutions.fornituras.modules.officers.service;

/**
 * Enmascara identificadores de PII (CURP/RFC) para roles no autorizados: muestra los primeros
 * caracteres y oculta el resto, de modo que un operador pueda cotejar parcialmente sin ver el dato
 * completo. La decisión de enmascarar la toma el servidor (nunca el cliente).
 */
public final class PiiMasker {

	private static final int VISIBLE_PREFIX = 4;

	private PiiMasker() {
	}

	public static String mask(String value) {
		if (value == null || value.isBlank()) {
			return value;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= VISIBLE_PREFIX) {
			return "•".repeat(trimmed.length());
		}
		String prefix = trimmed.substring(0, VISIBLE_PREFIX);
		return prefix + "•".repeat(trimmed.length() - VISIBLE_PREFIX);
	}
}
