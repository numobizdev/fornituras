package com.numobiz.solutions.fornituras.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Escritor de auditoría mínimo compartido para entidades sin PII (catálogos y datos maestros
 * como municipio o almacén).
 *
 * <p>La feature 012 (bitácora ISO 27001) aún no existe; cuando se implemente, este componente
 * se reemplaza por el puerto de auditoría definitivo. Hasta entonces se registra el evento por
 * SLF4J anotando solo actor, acción y el id del recurso (nunca datos sensibles).
 */
@Component
public class AuditWriter {

	private static final Logger log = LoggerFactory.getLogger(AuditWriter.class);

	public void record(String action, Long resourceId) {
		log.info("AUDIT action={} resourceId={} actor={}", action, resourceId, currentActor());
	}

	/**
	 * Registra un evento sin recurso puntual (p. ej. exportación de un reporte). El {@code detail}
	 * describe el contexto del evento (tipo de reporte, campos de filtro usados) y <b>nunca</b> debe
	 * contener PII ni valores de filtro sensibles (Principio V).
	 */
	public void recordEvent(String action, String detail) {
		log.info("AUDIT action={} detail={} actor={}", action, detail, currentActor());
	}

	private String currentActor() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication != null && authentication.isAuthenticated())
				? authentication.getName()
				: "anonymous";
	}
}
