package com.numobiz.solutions.fornituras.modules.landing.entity;

/**
 * Cara en la que se muestra una sección de contenido: {@code PUBLIC} (landing pública pre-login, sin
 * autenticación) u {@code HOME} (inicio del panel tras iniciar sesión). Actúa como discriminador de la
 * única tabla {@code landing_section} (ADR 0015): ambas caras comparten el mismo motor de configuración.
 */
public enum LandingScope {
	PUBLIC,
	HOME
}
