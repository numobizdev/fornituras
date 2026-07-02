package com.numobiz.solutions.fornituras.common.exception;

/** El contenido es sintácticamente válido pero no se puede procesar (p. ej. imagen no decodificable): HTTP 422. */
public class UnprocessableEntityException extends RuntimeException {

	public UnprocessableEntityException(String message) {
		super(message);
	}
}
