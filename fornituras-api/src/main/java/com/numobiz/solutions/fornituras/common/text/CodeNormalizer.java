package com.numobiz.solutions.fornituras.common.text;

import java.util.Locale;

/**
 * Normaliza códigos opacos (QR/serie) para comparar unicidad física de forma robusta: recorta,
 * elimina espacios y guiones internos y pasa a mayúsculas. Así "FOR-00001", "for 00001" y
 * "FOR00001" se consideran el mismo identificador y no generan duplicados "aparentemente
 * distintos".
 *
 * <p>A diferencia de {@link NameNormalizer} (nombres legibles: minúsculas, sin acentos), aquí los
 * códigos van en mayúsculas y se descartan separadores, porque el grabado/impresión puede variar.
 */
public final class CodeNormalizer {

	private CodeNormalizer() {
	}

	public static String normalize(String raw) {
		if (raw == null) {
			return "";
		}
		return raw.replaceAll("[\\s-]+", "").toUpperCase(Locale.ROOT);
	}
}
