package com.numobiz.solutions.fornituras.common.crypto;

import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

/**
 * Calcula el blind index {@code HMAC-SHA256(clave, normalize(valor))} de identificadores como CURP
 * y RFC (ADR 0004/0006): permite igualdad exacta sobre datos cifrados sin descifrarlos. La clave
 * viene de {@code OFFICER_BLIND_INDEX_KEY} (entorno; nunca en el repo — Principio III).
 */
@Component
public class BlindIndexer {

	private final byte[] key;

	public BlindIndexer(@Value("${app.pii.blind-index-key:}") String key) {
		this.key = (key == null) ? new byte[0] : key.trim().getBytes(StandardCharsets.UTF_8);
	}

	/** Devuelve el índice hex del valor normalizado, o {@code null} si el valor es nulo/vacío. */
	public String index(String raw) {
		String normalized = CodeNormalizer.normalize(raw);
		if (normalized.isBlank()) {
			return null;
		}
		if (key.length == 0) {
			throw new IllegalStateException(
					"OFFICER_BLIND_INDEX_KEY no configurada: el padrón requiere la clave del blind index.");
		}
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			byte[] digest = mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo calcular el blind index", e);
		}
	}
}
