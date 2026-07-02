package com.numobiz.solutions.fornituras.security;

import java.util.Set;

import org.springframework.security.core.Authentication;

/**
 * Matriz de autorización del sistema (RBAC) centralizada, según el
 * <a href="../../../../../../../../../docs/04-decisiones/0013-expansion-de-roles.md">ADR 0013</a>
 * (mínimo privilegio, rechazo por defecto).
 *
 * <p>Las constantes son expresiones SpEL para {@code @PreAuthorize}: cada controlador las <b>reutiliza</b>
 * en lugar de repetir literales de rol, de modo que ampliar o ajustar la matriz se hace en un solo lugar
 * y no queda ningún endpoint con la expresión antigua. La visibilidad de PII (regla 3), que es una
 * decisión en tiempo de ejecución y no una anotación, se resuelve con {@link #canViewFullPii(Authentication)}.
 */
public final class RolePolicy {

	private RolePolicy() {
	}

	/** Regla 1 — Escritura de inventario de fornituras (alta, edición, estado) y generación de QR. */
	public static final String WRITE_INVENTORY = "hasAnyRole('ADMIN','ALMACEN','CAPTURISTA')";

	/** Regla 2 — Traslados entre almacenes: mando los autoriza, almacén los administra, capturista captura. */
	public static final String WRITE_TRANSFERS = "hasAnyRole('ADMIN','SUPERVISOR','ALMACEN','CAPTURISTA')";

	/** Regla 2 — Asignaciones e incidencias sobre fornituras: mando autoriza, capturista captura. */
	public static final String WRITE_OPERATIONS = "hasAnyRole('ADMIN','SUPERVISOR','CAPTURISTA')";

	/** Regla 2 — Bajas definitivas: autorización de mando (acción elevada). */
	public static final String AUTHORIZE_DECOMMISSION = "hasAnyRole('ADMIN','SUPERVISOR')";

	/** Alta en el padrón de elementos (PII): mando o capturista; ALMACÉN excluido (sin PII completa). */
	public static final String WRITE_OFFICERS = "hasAnyRole('ADMIN','SUPERVISOR','CAPTURISTA')";

	/** Administración de configuración: catálogos y almacenes. Solo ADMIN. */
	public static final String MANAGE_CONFIG = "hasRole('ADMIN')";

	/** Administración del contenido de la landing (secciones públicas y de inicio). Solo ADMIN (016). */
	public static final String MANAGE_LANDING = "hasRole('ADMIN')";

	/** Regla 5 — Gestión de usuarios y roles. Solo ADMIN. */
	public static final String MANAGE_USERS = "hasRole('ADMIN')";

	/** Regla 4 — Consulta de la bitácora de auditoría (012). */
	public static final String READ_AUDIT = "hasAnyRole('ADMIN','AUDITOR')";

	/**
	 * Regla 3 — Autoridades que ven la PII completa (CURP/RFC en claro); cualquier otro rol la ve
	 * enmascarada. Se define sobre las autoridades de Spring ({@code ROLE_*}) para poder consultarla
	 * en tiempo de ejecución en los servicios que resuelven PII.
	 */
	private static final Set<String> FULL_PII_AUTHORITIES = Set.of("ROLE_ADMIN", "ROLE_SUPERVISOR", "ROLE_AUDITOR");

	/** Autoridades que pueden capturar datos del padrón de elementos (alta/edición, {@link #WRITE_OFFICERS}). */
	private static final Set<String> WRITE_OFFICER_AUTHORITIES = Set.of("ROLE_ADMIN", "ROLE_SUPERVISOR", "ROLE_CAPTURISTA");

	/** {@code true} si el actor autenticado puede ver la PII sin enmascarar (regla 3). */
	public static boolean canViewFullPii(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> FULL_PII_AUTHORITIES.contains(authority.getAuthority()));
	}

	/**
	 * {@code true} si el actor puede subir la foto de un elemento (PII). Coincide con quienes capturan
	 * el padrón ({@link #WRITE_OFFICERS}); la <b>visualización</b> de la foto se rige por la regla 3
	 * ({@link #canViewFullPii}), de modo que un capturista puede adjuntarla pero la ve enmascarada.
	 */
	public static boolean canUploadOfficerPhoto(Authentication authentication) {
		return authentication != null && authentication.getAuthorities().stream()
				.anyMatch(authority -> WRITE_OFFICER_AUTHORITIES.contains(authority.getAuthority()));
	}
}
