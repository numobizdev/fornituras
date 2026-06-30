package com.numobiz.solutions.fornituras.modules.qrcodes.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lote_qr")
public class LoteQR extends BaseEntity {

	@Column(name = "consecutivo_inicial", nullable = false)
	private int consecutivoInicial;

	@Column(name = "consecutivo_final", nullable = false)
	private int consecutivoFinal;

	@Column(nullable = false)
	private int cantidad;

	@Column(nullable = false, length = 255)
	private String descripcion;

	@Column(name = "qr_size_cm", nullable = false, precision = 5, scale = 2)
	private BigDecimal qrSizeCm;

	@Column(name = "padding_cm", nullable = false, precision = 5, scale = 2)
	private BigDecimal paddingCm;

	@Enumerated(EnumType.STRING)
	@Column(name = "label_position", nullable = false, length = 10)
	private LabelPosition labelPosition;

	@Column(name = "mostrar_bordes", nullable = false)
	private boolean mostrarBordes = true;
}
