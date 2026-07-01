package com.numobiz.solutions.fornituras.common.exception;

/**
 * Se superó el límite de peticiones permitido para la operación (HTTP 429). P. ej. demasiadas
 * resoluciones de código por el mismo actor en poco tiempo (mitiga la enumeración, ADR 0005/0010).
 */
public class TooManyRequestsException extends RuntimeException {

	public TooManyRequestsException(String message) {
		super(message);
	}
}
