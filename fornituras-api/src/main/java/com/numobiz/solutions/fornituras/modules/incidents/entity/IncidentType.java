package com.numobiz.solutions.fornituras.modules.incidents.entity;

/**
 * Tipo de incidencia sobre una fornitura. {@code EXTRAVIO} deriva la fornitura a
 * {@code EXTRAVIADA}; los demás (daño, falla, mantenimiento) la derivan a {@code EN_MANTENIMIENTO}.
 * Persiste como cadena y se valida con un {@code CHECK} en la migración.
 */
public enum IncidentType {
	DANO,
	FALLA,
	EXTRAVIO,
	MANTENIMIENTO
}
