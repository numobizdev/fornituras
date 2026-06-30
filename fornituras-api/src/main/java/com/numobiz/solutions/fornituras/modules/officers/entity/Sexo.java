package com.numobiz.solutions.fornituras.modules.officers.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Catálogo de sexo (dato de referencia, no PII por sí mismo). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sexo")
public class Sexo extends BaseEntity {

	@Column(nullable = false, length = 30)
	private String nombre;

	@Column(nullable = false)
	private boolean active = true;
}
