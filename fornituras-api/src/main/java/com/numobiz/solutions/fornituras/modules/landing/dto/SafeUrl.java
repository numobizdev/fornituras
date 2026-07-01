package com.numobiz.solutions.fornituras.modules.landing.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Restricción de esquema de URL segura (anti-XSS, ADR 0015). Un valor es válido si es nulo/vacío
 * (opcional), una ruta interna relativa que empiece por {@code /} (no {@code //}), o una URL
 * {@code http}/{@code https}. Rechaza {@code javascript:}, {@code data:} y esquemas peligrosos.
 */
@Documented
@Constraint(validatedBy = SafeUrlValidator.class)
@Target({FIELD, RECORD_COMPONENT})
@Retention(RUNTIME)
public @interface SafeUrl {

	String message() default "La URL debe ser una ruta interna (/...) o http/https";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};
}
