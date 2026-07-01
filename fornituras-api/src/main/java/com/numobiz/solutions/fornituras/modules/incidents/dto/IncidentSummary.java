package com.numobiz.solutions.fornituras.modules.incidents.dto;

import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentType;

import java.time.LocalDateTime;

/** Fila/ficha de incidencia para listado y detalle. Incluye el código de la fornitura. Sin PII. */
public record IncidentSummary(
		Long id,
		Long equipmentId,
		String equipmentCodigo,
		IncidentType tipo,
		String descripcion,
		IncidentStatus estado,
		LocalDateTime fechaReporte,
		LocalDateTime fechaResolucion
) {
}
