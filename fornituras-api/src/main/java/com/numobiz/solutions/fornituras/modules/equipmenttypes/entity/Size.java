package com.numobiz.solutions.fornituras.modules.equipmenttypes.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "size")
public class Size extends BaseEntity {

	@Column(nullable = false, length = 50)
	private String etiqueta;

	/**
	 * Talla opcionalmente asociada a un tipo: NULL = talla global; un chaleco y un
	 * cinturón no comparten tallas, así que el catálogo permite acotar por tipo.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "equipment_type_id")
	private EquipmentType equipmentType;

	@Column(nullable = false)
	private boolean active = true;
}
