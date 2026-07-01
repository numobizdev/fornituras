package com.numobiz.solutions.fornituras.modules.catalog;

/**
 * Claves ({@code catalog.code}) de los catálogos de sistema. Centralizadas para que los módulos
 * consumidores no repitan strings mágicos al resolver sus referencias (ADR 0007).
 */
public final class CatalogCodes {

	public static final String TIPO_PRENDA = "TIPO_PRENDA";
	public static final String TALLA = "TALLA";
	public static final String TIPO_ALMACEN = "TIPO_ALMACEN";
	public static final String SEXO = "SEXO";
	public static final String TIPO_SANGRE = "TIPO_SANGRE";

	private CatalogCodes() {
	}
}
