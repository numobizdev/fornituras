package com.numobiz.solutions.fornituras.modules.incidents.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Incidencia registrada sobre una fornitura (daño, falla, extravío, mantenimiento). Referencia la
 * fornitura y los usuarios por id para mantener el módulo desacoplado; sin PII. El estado gobierna el
 * ciclo (abierta → en proceso → resuelta/cerrada); reportarla puede retirar la fornitura y resolverla
 * puede devolverla a disponible, siempre de forma transaccional y auditada.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "incident")
public class Incident extends BaseEntity {

	@Column(name = "equipment_id", nullable = false)
	private Long equipmentId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IncidentType tipo;

	@Column(nullable = false, length = 500)
	private String descripcion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private IncidentStatus estado = IncidentStatus.ABIERTA;

	@Column(name = "fecha_reporte", nullable = false)
	private LocalDateTime fechaReporte;

	@Column(name = "fecha_resolucion")
	private LocalDateTime fechaResolucion;

	@Column(name = "reportado_por")
	private Long reportadoPor;

	@Column(name = "actualizado_por")
	private Long actualizadoPor;
}
