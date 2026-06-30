package com.numobiz.solutions.fornituras.modules.officers.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo de tipo de sangre (O±/A±/B±/AB±). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tipo_sangre")
public class TipoSangre extends BaseEntity {

	@Column(nullable = false, length = 5)
	private String etiqueta;

	@Column(nullable = false)
	private boolean active = true;
}
