package com.numobiz.solutions.fornituras.common.exception;

/**
 * Conflicto con el estado actual del recurso (HTTP 409). P. ej. nombre único duplicado.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
