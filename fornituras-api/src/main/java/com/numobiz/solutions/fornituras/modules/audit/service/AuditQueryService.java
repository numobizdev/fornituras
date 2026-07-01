package com.numobiz.solutions.fornituras.modules.audit.service;

import com.numobiz.solutions.fornituras.modules.audit.dto.AuditLogSummary;
import com.numobiz.solutions.fornituras.modules.audit.entity.AuditLog;
import com.numobiz.solutions.fornituras.modules.audit.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta de la bitácora (US1): paginada y filtrable por actor, acción, entidad y rango de fechas,
 * apoyada en los índices de {@code audit_log} (SC-004). Solo lectura; normaliza los filtros antes de
 * delegar en el repositorio append-only.
 */
@Service
@Transactional(readOnly = true)
public class AuditQueryService {

	private final AuditLogRepository repository;

	public AuditQueryService(AuditLogRepository repository) {
		this.repository = repository;
	}

	public Page<AuditLogSummary> query(
			String actor, String accion, String entidad,
			LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
		return repository.search(
						likeLowerOrNull(actor),
						upperOrNull(accion),
						upperOrNull(entidad),
						desde, hasta, pageable)
				.map(this::toSummary);
	}

	private AuditLogSummary toSummary(AuditLog log) {
		return new AuditLogSummary(
				log.getId(),
				log.getUsuarioId(),
				log.getActor(),
				log.getAccion(),
				log.getEntidad(),
				log.getEntidadId(),
				log.getOccurredAt(),
				log.getIp(),
				log.getEvidencia());
	}

	private String likeLowerOrNull(String value) {
		return blank(value) ? null : "%" + value.trim().toLowerCase(Locale.ROOT) + "%";
	}

	private String upperOrNull(String value) {
		return blank(value) ? null : value.trim().toUpperCase(Locale.ROOT);
	}

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
