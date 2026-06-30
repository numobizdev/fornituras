package com.numobiz.solutions.fornituras.modules.equipmenttypes.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Escritor de auditoría mínimo para el catálogo de tipos/tallas.
 *
 * <p>La feature 012 (bitácora ISO 27001) aún no existe; cuando se implemente, este
 * componente se reemplaza por el puerto {@code AuditWriter} compartido. Hasta entonces se
 * registra el evento por SLF4J. El catálogo no contiene PII, así que solo se anota actor,
 * acción y el id del recurso (nunca datos sensibles).
 */
@Component
public class CatalogAuditWriter {

	private static final Logger log = LoggerFactory.getLogger(CatalogAuditWriter.class);

	public void record(String action, Long resourceId) {
		log.info("AUDIT action={} resourceId={} actor={}", action, resourceId, currentActor());
	}

	private String currentActor() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication != null && authentication.isAuthenticated())
				? authentication.getName()
				: "anonymous";
	}
}
