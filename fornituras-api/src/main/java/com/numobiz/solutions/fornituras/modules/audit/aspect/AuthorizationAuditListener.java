package com.numobiz.solutions.fornituras.modules.audit.aspect;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import org.springframework.context.event.EventListener;
import org.springframework.security.authorization.event.AuthorizationDeniedEvent;
import org.springframework.stereotype.Component;

/**
 * Captura automática de accesos denegados (FR-006): cuando la seguridad de método rechaza una
 * operación por autorización, se registra un evento {@code ACCESS_DENIED} en la bitácora (el actor y
 * la IP los resuelve el {@link AuditWriter} desde el contexto). Solo se publican denegaciones, así
 * que no hay doble registro de accesos concedidos.
 */
@Component
public class AuthorizationAuditListener {

	private final AuditWriter audit;

	public AuthorizationAuditListener(AuditWriter audit) {
		this.audit = audit;
	}

	@EventListener
	public void onAuthorizationDenied(AuthorizationDeniedEvent<?> event) {
		audit.recordEvent("ACCESS_DENIED", "authorization=denied");
	}
}
