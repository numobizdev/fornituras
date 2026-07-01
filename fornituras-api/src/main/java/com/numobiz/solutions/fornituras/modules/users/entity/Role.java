package com.numobiz.solutions.fornituras.modules.users.entity;

/**
 * Roles del sistema (RBAC, mínimo privilegio). La matriz rol→permisos y su propagación se fijan en el
 * <a href="../../../../../../../../../docs/04-decisiones/0013-expansion-de-roles.md">ADR 0013</a> y se
 * materializan en las expresiones de {@code RolePolicy}. No se relaja ningún control al ampliar el enum
 * (rechazo por defecto). Los usuarios existentes conservan {@code ADMIN}/{@code CAPTURISTA}; no hay
 * migración de datos obligatoria.
 */
public enum Role {
	/** Control total: usuarios, roles, configuración y todas las operaciones. */
	ADMIN,
	/** Consulta completa; autoriza altas-bajas, asignaciones y traslados; no administra usuarios. */
	SUPERVISOR,
	/** Administra inventario (alta/edición de fornituras) y traslados; sin gestión de usuarios ni PII completa. */
	ALMACEN,
	/** Solo lectura: consultas, reportes y bitácora de auditoría; sin escritura. */
	AUDITOR,
	/** Operación limitada (captura/consulta/asignación); sin PII completa ni administración. */
	CAPTURISTA
}
