package com.numobiz.solutions.fornituras.modules.assignments.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Asignación (resguardo): relación fornitura↔elemento en el tiempo.
 *
 * <p>{@code fechaDevolucion == null} significa <b>vigente</b>; con valor es histórico. Un índice
 * único filtrado en BD garantiza <b>una sola asignación vigente por fornitura</b> (la garantía
 * dura frente a concurrencia). Las referencias a fornitura/elemento/usuarios se guardan por id
 * para mantener el módulo desacoplado; sin PII (el nombre del elemento se resuelve vía 003).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assignment")
public class Assignment extends BaseEntity {

	@Column(name = "equipment_id", nullable = false)
	private Long equipmentId;

	@Column(name = "officer_id", nullable = false)
	private Long officerId;

	@Column(name = "fecha_asignacion", nullable = false)
	private LocalDateTime fechaAsignacion;

	/** NULL = asignación vigente; con valor = devuelta (histórico). */
	@Column(name = "fecha_devolucion")
	private LocalDateTime fechaDevolucion;

	@Column(name = "asignado_por")
	private Long asignadoPor;

	@Column(name = "recibido_por")
	private Long recibidoPor;

	@Column(name = "firma_url", length = 500)
	private String firmaUrl;

	@Column(length = 500)
	private String observaciones;
}
