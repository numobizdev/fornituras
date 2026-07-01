package com.numobiz.solutions.fornituras.modules.officers.mapper;

import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerDetail;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerSummary;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.service.PiiMasker;
import org.springframework.stereotype.Component;

/**
 * Mapea {@link Officer} a sus DTOs aplicando las reglas de enmascaramiento de PII por rol. La
 * decisión de mostrar CURP/RFC en claro la toma el servidor a través de {@code unmaskPii}; el
 * frontend nunca recibe el dato completo si el rol no está autorizado.
 */
@Component
public class OfficerMapper {

	public OfficerSummary toSummary(
			Officer officer, String sexoNombre, String tipoSangreEtiqueta) {
		return new OfficerSummary(
				officer.getId(),
				fullName(officer),
				officer.getPlaca(),
				sexoNombre,
				tipoSangreEtiqueta,
				officer.getMunicipio(),
				officer.getEstado(),
				officer.getFotoUrl(),
				officer.isActive());
	}

	public OfficerDetail toDetail(
			Officer officer, String sexoNombre, String tipoSangreEtiqueta, boolean unmaskPii) {
		String curp = unmaskPii ? officer.getCurp() : PiiMasker.mask(officer.getCurp());
		String rfc = unmaskPii ? officer.getRfc() : PiiMasker.mask(officer.getRfc());
		return new OfficerDetail(
				officer.getId(),
				officer.getNombre(),
				officer.getApellidoPaterno(),
				officer.getApellidoMaterno(),
				fullName(officer),
				officer.getPlaca(),
				officer.getSexoId(), sexoNombre,
				officer.getTipoSangreId(), tipoSangreEtiqueta,
				officer.getMunicipio(), officer.getEstado(),
				curp, rfc, !unmaskPii,
				officer.getFotoUrl(),
				officer.isActive(),
				officer.getCreatedAt(),
				officer.getUpdatedAt());
	}

	private String fullName(Officer officer) {
		StringBuilder sb = new StringBuilder(officer.getNombre()).append(' ').append(officer.getApellidoPaterno());
		if (officer.getApellidoMaterno() != null && !officer.getApellidoMaterno().isBlank()) {
			sb.append(' ').append(officer.getApellidoMaterno());
		}
		return sb.toString();
	}
}
