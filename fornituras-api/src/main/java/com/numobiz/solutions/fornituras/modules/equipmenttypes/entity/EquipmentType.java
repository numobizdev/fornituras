package com.numobiz.solutions.fornituras.modules.equipmenttypes.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "equipment_type")
public class EquipmentType extends BaseEntity {

	@Column(nullable = false, length = 120)
	private String nombre;

	/**
	 * Nombre normalizado (trim + colapso de espacios + casefold) usado para garantizar
	 * unicidad case/space-insensitive. El usuario nunca lo ve; solo se compara.
	 */
	@Column(name = "nombre_normalizado", nullable = false, unique = true, length = 120)
	private String nombreNormalizado;

	@Column(length = 500)
	private String descripcion;

	@Column(name = "foto_url", length = 500)
	private String fotoUrl;

	@Column(nullable = false)
	private boolean active = true;
}
