package com.numobiz.solutions.fornituras.common.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * Inyecta la clave de cifrado de PII en {@link PiiCipher} al arrancar, desde la variable de entorno
 * {@code PII_ENCRYPTION_KEY} (Base64). Si no está configurada, el arranque no falla (otros módulos
 * funcionan), pero cualquier persistencia/lectura de PII lanzará un error claro hasta que se defina.
 */
@Component
public class PiiCipherInitializer {

	private static final Logger log = LoggerFactory.getLogger(PiiCipherInitializer.class);

	public PiiCipherInitializer(@Value("${app.pii.encryption-key:}") String base64Key) {
		if (base64Key == null || base64Key.isBlank()) {
			log.warn("PII_ENCRYPTION_KEY no configurada: el padrón de elementos no podrá cifrar/descifrar PII.");
			return;
		}
		byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
		if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
			throw new IllegalStateException("PII_ENCRYPTION_KEY debe ser una clave AES de 16, 24 o 32 bytes (Base64).");
		}
		PiiCipher.configure(keyBytes);
		log.info("Cifrado de PII a nivel de aplicación inicializado (AES-GCM).");
	}
}
