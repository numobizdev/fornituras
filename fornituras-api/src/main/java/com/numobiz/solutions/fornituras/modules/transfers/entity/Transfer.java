package com.numobiz.solutions.fornituras.modules.transfers.entity;

import com.numobiz.solutions.fornituras.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Traslado de fornituras entre dos almacenes. Referencia origen/destino y usuarios por id para
 * mantener el módulo desacoplado; sin PII. Las fornituras trasladadas viven en {@link TransferItem}.
 * El estado gobierna las transiciones (enviado→recibido/cancelado), siempre transaccionales.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transfer")
public class Transfer extends BaseEntity {

	@Column(name = "origen_id", nullable = false)
	private Long origenId;

	@Column(name = "destino_id", nullable = false)
	private Long destinoId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TransferStatus status = TransferStatus.ENVIADO;

	@Column(name = "fecha_envio", nullable = false)
	private LocalDateTime fechaEnvio;

	@Column(name = "fecha_recepcion")
	private LocalDateTime fechaRecepcion;

	@Column(name = "creado_por")
	private Long creadoPor;

	@Column(name = "recibido_por")
	private Long recibidoPor;

	@Column(length = 500)
	private String observaciones;
}
