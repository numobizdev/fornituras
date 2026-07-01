package com.numobiz.solutions.fornituras.modules.decommissions.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Motivo de baja (catálogo controlado: caducidad, daño, extravío, obsolescencia…). Se siembra en la
 * migración y puede desactivarse ({@code active=false}) sin borrarlo para conservar el historial de
 * bajas que lo referencian. Sin PII.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "decommission_reason")
public class DecommissionReason extends BaseEntity {

	@Column(nullable = false, length = 100)
	private String nombre;

	@Column(nullable = false)
	private boolean active = true;
}
