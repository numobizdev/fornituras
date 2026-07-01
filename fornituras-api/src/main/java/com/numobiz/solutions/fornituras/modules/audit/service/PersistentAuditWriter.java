package com.numobiz.solutions.fornituras.modules.audit.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.modules.audit.entity.AuditLog;
import com.numobiz.solutions.fornituras.modules.audit.repository.AuditLogRepository;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Implementación persistente del puerto {@link AuditWriter} (feature 012): escribe cada evento en la
 * bitácora append-only, resolviendo actor (email + id de usuario) e IP del contexto, y <b>redactando
 * PII</b> del detalle. Persiste en una transacción <b>propia</b> ({@code REQUIRES_NEW}) para que el
 * registro quede aunque el llamador esté en una transacción de solo lectura o esta luego falle: la
 * auditoría no debe perderse (SC-001) ni depender del resultado de la operación.
 */
@Service
public class PersistentAuditWriter implements AuditWriter {

	private static final Logger log = LoggerFactory.getLogger(PersistentAuditWriter.class);

	private final AuditLogRepository repository;
	private final UserRepository userRepository;

	public PersistentAuditWriter(AuditLogRepository repository, UserRepository userRepository) {
		this.repository = repository;
		this.userRepository = userRepository;
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(String action, Long resourceId) {
		persist(action, null, resourceId, null);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordEvent(String action, String detail) {
		persist(action, null, null, AuditRedactor.redact(detail));
	}

	private void persist(String action, String entidad, Long entidadId, String evidencia) {
		try {
			AuditLog entry = new AuditLog();
			String actor = currentActor();
			entry.setActor(actor);
			entry.setUsuarioId(resolveUserId(actor));
			entry.setAccion(action);
			entry.setEntidad(entidad);
			entry.setEntidadId(entidadId);
			entry.setOccurredAt(LocalDateTime.now());
			entry.setIp(currentIp());
			entry.setEvidencia(evidencia);
			repository.save(entry);
		} catch (RuntimeException e) {
			// La auditoría no debe tumbar la petición; se deja rastro por log para no perder señal.
			log.error("No se pudo persistir el evento de auditoría action={} : {}", action, e.getMessage());
		}
	}

	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication != null && authentication.isAuthenticated())
				? authentication.getName()
				: "anonymous";
	}

	private Long resolveUserId(String actor) {
		if (actor == null || actor.isBlank()) {
			return null;
		}
		return userRepository.findByEmail(actor).map(User::getId).orElse(null);
	}

	private String currentIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes.getRequest().getRemoteAddr();
		}
		return null;
	}
}
