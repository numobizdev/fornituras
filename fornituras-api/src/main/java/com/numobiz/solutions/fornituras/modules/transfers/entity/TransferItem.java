package com.numobiz.solutions.fornituras.modules.transfers.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Línea de un traslado: una fornitura incluida en él. Referencia por id (desacople); la unicidad de
 * "una fornitura en un solo traslado en curso" se refuerza en la lógica de servicio y con el estado
 * {@code EN_TRASLADO} de la propia fornitura.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transfer_item")
public class TransferItem extends BaseEntity {

	@Column(name = "transfer_id", nullable = false)
	private Long transferId;

	@Column(name = "equipment_id", nullable = false)
	private Long equipmentId;
}
