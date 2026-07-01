package com.numobiz.solutions.fornituras.modules.landing.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sección de contenido configurable de la landing (feature 016). Una fila representa un bloque de una de
 * las dos caras ({@link LandingScope}) con un {@link LandingSectionType}. Todo el texto se almacena
 * literal (sin marcado ejecutable) y se escapa en el render (ADR 0015); las URLs se validan en el borde.
 * Los accesos rápidos se guardan como JSON en {@code config_json}. La baja es lógica ({@code active}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "landing_section")
public class LandingSection extends BaseEntity {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private LandingScope scope;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LandingSectionType type;

	@Column(length = 160)
	private String titulo;

	@Column(length = 240)
	private String subtitulo;

	@Column(length = 2000)
	private String cuerpo;

	@Column(name = "imagen_url", length = 512)
	private String imagenUrl;

	@Column(name = "cta_label", length = 80)
	private String ctaLabel;

	@Column(name = "cta_url", length = 512)
	private String ctaUrl;

	@Column(nullable = false)
	private int orden = 0;

	@Column(nullable = false)
	private boolean active = true;

	/** JSON de {@code QuickLinkItem[]} para {@code QUICK_LINKS}; null en los demás tipos. */
	@Column(name = "config_json", length = 4000)
	private String configJson;
}
