package com.numobiz.solutions.fornituras.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Cifra/descifra PII a nivel de aplicación con AES-256-GCM (ADR 0006, interino hasta Always
 * Encrypted). El valor persistido es {@code Base64(IV ‖ ciphertext+tag)}; cada cifrado usa un IV
 * aleatorio (cifrado no determinista), por lo que estas columnas no soportan búsqueda por igualdad
 * (para eso existe el blind index de {@code curp}/{@code rfc}).
 *
 * <p>La clave se inyecta una vez al arranque ({@link #configure(byte[])}) desde el entorno; nunca
 * vive en el repositorio (Principio III). Se usa un holder estático porque el
 * {@link EncryptedStringConverter} lo instancia Hibernate, fuera del contenedor de Spring.
 */
public final class PiiCipher {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int IV_LENGTH = 12;
	private static final int TAG_BITS = 128;
	private static final SecureRandom RANDOM = new SecureRandom();

	private static volatile SecretKey key;

	private PiiCipher() {
	}

	/** Configura la clave AES (16/24/32 bytes). Idempotente; la llama el inicializador al arranque. */
	public static void configure(byte[] keyBytes) {
		key = new SecretKeySpec(keyBytes.clone(), "AES");
	}

	public static boolean isConfigured() {
		return key != null;
	}

	public static String encrypt(String plain) {
		if (plain == null) {
			return null;
		}
		SecretKey currentKey = requireKey();
		try {
			byte[] iv = new byte[IV_LENGTH];
			RANDOM.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, currentKey, new GCMParameterSpec(TAG_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(combined);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo cifrar el dato de PII", e);
		}
	}

	public static String decrypt(String stored) {
		if (stored == null) {
			return null;
		}
		SecretKey currentKey = requireKey();
		try {
			byte[] combined = Base64.getDecoder().decode(stored);
			byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
			byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, currentKey, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("No se pudo descifrar el dato de PII", e);
		}
	}

	private static SecretKey requireKey() {
		SecretKey currentKey = key;
		if (currentKey == null) {
			throw new IllegalStateException(
					"PII_ENCRYPTION_KEY no configurada: el padrón de elementos requiere la clave de cifrado de PII.");
		}
		return currentKey;
	}
}
