package com.numobiz.solutions.fornituras.modules.incidents.entity;

/**
 * Estado de seguimiento de una incidencia. Al resolver/cerrar, la fornitura afectada puede volver a
 * {@code DISPONIBLE} si estaba retirada por la incidencia. Persiste como cadena (CHECK en migración).
 */
public enum IncidentStatus {
	ABIERTA,
	EN_PROCESO,
	RESUELTA,
	CERRADA
}
