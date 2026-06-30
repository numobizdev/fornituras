package com.numobiz.solutions.fornituras.modules.equipmenttypes.service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normaliza nombres para comparar unicidad de forma robusta: recorta, colapsa espacios
 * internos, quita acentos (NFD) y pasa a minúsculas. "Chaleco  Antibala" y "chaleco
 * antibalá " se consideran el mismo nombre.
 */
public final class NameNormalizer {

	private NameNormalizer() {
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		String collapsed = raw.trim().replaceAll("\\s+", " ");
		String withoutAccents = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "");
		return withoutAccents.toLowerCase(Locale.ROOT);
	}
}
