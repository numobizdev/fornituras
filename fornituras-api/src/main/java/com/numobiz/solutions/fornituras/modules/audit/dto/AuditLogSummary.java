package com.numobiz.solutions.fornituras.modules.audit.dto;

import java.time.LocalDateTime;

/**
 * Fila de lectura de la bitácora (012). Solo datos de trazabilidad: quién, qué, sobre qué (por id),
 * cuándo, desde dónde y evidencia redactada. Nunca PII en claro.
 */
public record AuditLogSummary(
		Long id,
		Long usuarioId,
		String actor,
		String accion,
		String entidad,
		Long entidadId,
		LocalDateTime occurredAt,
		String ip,
		String evidencia) {
}
