package com.numobiz.solutions.fornituras.modules.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Evento de la bitácora de auditoría (012). Registro <b>append-only</b>: no extiende
 * {@code BaseEntity} porque no tiene {@code updatedAt} (nunca se modifica). Referencia a la entidad
 * afectada por {@code entidad} + {@code entidadId} (nunca PII en claro); {@code evidencia} guarda un
 * detalle redactado. {@code prevHash} queda reservado para el encadenamiento por hash (ADR 0012).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Id del usuario de la app que ejecutó la acción (FK lógica a {@code users}); null si anónimo. */
	@Column(name = "usuario_id")
	private Long usuarioId;

	/** Identidad del actor (email de la cuenta) para trazabilidad; null/"anonymous" si no autenticado. */
	@Column(length = 150)
	private String actor;

	@Column(nullable = false, length = 80)
	private String accion;

	@Column(length = 60)
	private String entidad;

	@Column(name = "entidad_id")
	private Long entidadId;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	@Column(length = 45)
	private String ip;

	/** Detalle/evidencia del evento, ya redactado (sin PII ni secretos). */
	@Column(length = 1000)
	private String evidencia;

	/** Reservado para encadenamiento por hash (detección de manipulación); ADR 0012. */
	@Column(name = "prev_hash", length = 64)
	private String prevHash;
}
