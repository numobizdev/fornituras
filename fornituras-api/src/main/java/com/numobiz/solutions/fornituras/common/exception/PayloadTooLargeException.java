package com.numobiz.solutions.fornituras.common.exception;

/** El contenido subido excede el límite de peso permitido (HTTP 413). */
public class PayloadTooLargeException extends RuntimeException {

	public PayloadTooLargeException(String message) {
		super(message);
	}
}
