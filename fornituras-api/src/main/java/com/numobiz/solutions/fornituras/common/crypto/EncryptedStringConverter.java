package com.numobiz.solutions.fornituras.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte de forma transparente las columnas de PII en claro a su forma cifrada (ADR 0006).
 * Se aplica explícitamente con {@code @Convert} en los campos sensibles de {@code Officer}; no es
 * {@code autoApply} para no cifrar cadenas que no lo necesitan.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return PiiCipher.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return PiiCipher.decrypt(dbData);
	}
}
