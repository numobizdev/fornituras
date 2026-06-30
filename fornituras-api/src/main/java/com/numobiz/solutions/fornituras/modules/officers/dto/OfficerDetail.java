package com.numobiz.solutions.fornituras.modules.officers.dto;

import java.time.LocalDateTime;

/**
 * Ficha del elemento. {@code curp}/{@code rfc} llegan <b>enmascarados</b> salvo para roles
 * autorizados; {@code piiEnmascarada} indica si se ocultó PII. El acceso a esta ficha se audita.
 */
public record OfficerDetail(
		Long id,
		String nombre,
		String apellidoPaterno,
		String apellidoMaterno,
		String nombreCompleto,
		String placa,
		Long sexoId,
		String sexoNombre,
		Long tipoSangreId,
		String tipoSangreEtiqueta,
		Long municipioId,
		String municipioNombre,
		String curp,
		String rfc,
		boolean piiEnmascarada,
		String fotoUrl,
		boolean active,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {
}
