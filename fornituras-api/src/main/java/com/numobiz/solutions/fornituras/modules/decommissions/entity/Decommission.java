package com.numobiz.solutions.fornituras.modules.decommissions.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Baja definitiva de una fornitura. Referencia la fornitura y el usuario responsable por id (módulo
 * desacoplado, sin PII) y el motivo por el catálogo {@link DecommissionReason}. Una baja no se
 * revierte: un error se corrige con un ajuste auditado. El cambio de estado de la fornitura a
 * "baja definitiva" lo gobierna {@code EquipmentService} (única fuente de las transiciones, 001).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "decommission")
public class Decommission extends BaseEntity {

	@Column(name = "equipment_id", nullable = false)
	private Long equipmentId;

	@Column(name = "motivo_id", nullable = false)
	private Long motivoId;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(name = "responsable")
	private Long responsable;

	@Column(length = 500)
	private String observaciones;
}
