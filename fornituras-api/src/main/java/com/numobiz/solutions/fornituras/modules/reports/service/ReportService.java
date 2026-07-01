package com.numobiz.solutions.fornituras.modules.reports.service;

import com.numobiz.solutions.fornituras.common.crypto.BlindIndexer;
import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository.StatusTally;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.repository.IncidentRepository;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.repository.OfficerRepository;
import com.numobiz.solutions.fornituras.modules.officers.service.PiiMasker;
import com.numobiz.solutions.fornituras.modules.reports.dto.ActiveAssignmentFilter;
import com.numobiz.solutions.fornituras.modules.reports.dto.ActiveAssignmentRow;
import com.numobiz.solutions.fornituras.modules.reports.dto.ReportTotals;
import com.numobiz.solutions.fornituras.modules.reports.repository.ReportRepository;
import com.numobiz.solutions.fornituras.security.RolePolicy;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lógica de la vista de control (011): totales por estado (agregados, coinciden con 010) y
 * asignaciones activas con filtros. <b>Reutiliza el enmascaramiento de PII de 003</b>: CURP/RFC solo
 * viajan en claro para los roles autorizados (ADR 0013 regla 3); el resto los recibe enmascarados. La
 * misma lista filtrada alimenta la pantalla (paginada) y la exportación, garantizando que coincidan.
 */
@Service
@Transactional(readOnly = true)
public class ReportService {

	private static final List<IncidentStatus> ACTIVE_INCIDENTS =
			List.of(IncidentStatus.ABIERTA, IncidentStatus.EN_PROCESO);

	private final ReportRepository reportRepository;
	private final EquipmentRepository equipmentRepository;
	private final IncidentRepository incidentRepository;
	private final OfficerRepository officerRepository;
	private final BlindIndexer blindIndexer;

	public ReportService(
			ReportRepository reportRepository,
			EquipmentRepository equipmentRepository,
			IncidentRepository incidentRepository,
			OfficerRepository officerRepository,
			BlindIndexer blindIndexer) {
		this.reportRepository = reportRepository;
		this.equipmentRepository = equipmentRepository;
		this.incidentRepository = incidentRepository;
		this.officerRepository = officerRepository;
		this.blindIndexer = blindIndexer;
	}

	public ReportTotals totals() {
		Map<EquipmentStatus, Long> byStatus = new EnumMap<>(EquipmentStatus.class);
		for (StatusTally tally : equipmentRepository.tallyByStatus()) {
			byStatus.put(tally.getStatus(), tally.getTotal());
		}
		long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
		return new ReportTotals(
				total,
				byStatus.getOrDefault(EquipmentStatus.DISPONIBLE, 0L),
				byStatus.getOrDefault(EquipmentStatus.ASIGNADA, 0L),
				byStatus.getOrDefault(EquipmentStatus.EN_MANTENIMIENTO, 0L),
				incidentRepository.countDistinctEquipmentByEstadoIn(ACTIVE_INCIDENTS),
				byStatus.getOrDefault(EquipmentStatus.BAJA_DEFINITIVA, 0L),
				officerRepository.count());
	}

	/** Página de asignaciones activas: corta en memoria la lista filtrada (bounded: solo vigentes). */
	public Page<ActiveAssignmentRow> activeAssignments(ActiveAssignmentFilter filter, Pageable pageable) {
		List<ActiveAssignmentRow> all = activeAssignmentRows(filter);
		int from = Math.min((int) pageable.getOffset(), all.size());
		int to = Math.min(from + pageable.getPageSize(), all.size());
		return new PageImpl<>(all.subList(from, to), pageable, all.size());
	}

	/**
	 * Lista completa de asignaciones activas que cumplen el filtro, ya enmascarada por rol y ordenada.
	 * Es la fuente única para pantalla y exportación (garantiza que el Excel = la vista).
	 */
	public List<ActiveAssignmentRow> activeAssignmentRows(ActiveAssignmentFilter filter) {
		boolean unmask = canViewPii();
		String nombreNeedle = normalizeText(filter.nombre());
		return reportRepository.findActiveAssignments(
						likeOrNull(CodeNormalizer.normalize(safe(filter.qr()))),
						likeOrNull(CodeNormalizer.normalize(safe(filter.placa()))),
						blindIndexer.index(blankToNull(filter.curp())),
						blindIndexer.index(blankToNull(filter.rfc())),
						likeUpperOrNull(filter.municipio()))
				.stream()
				.map(row -> toRow(row, unmask))
				.filter(r -> nombreNeedle == null
						|| (r.elementoNombre() != null
								&& r.elementoNombre().toUpperCase(Locale.ROOT).contains(nombreNeedle)))
				.toList();
	}

	private ActiveAssignmentRow toRow(Object[] row, boolean unmask) {
		Assignment a = (Assignment) row[0];
		Equipment e = (Equipment) row[1];
		Officer o = (Officer) row[2];
		return new ActiveAssignmentRow(
				a.getId(),
				e.getId(),
				e.getCodigoQr(),
				e.getDescripcion(),
				o.getId(),
				fullName(o),
				o.getPlaca(),
				unmask ? o.getCurp() : PiiMasker.mask(o.getCurp()),
				unmask ? o.getRfc() : PiiMasker.mask(o.getRfc()),
				o.getMunicipio(),
				o.getEstado(),
				!unmask,
				a.getFechaAsignacion());
	}

	/** El servidor decide la visibilidad de la PII a partir del rol (solo ADMIN ve CURP/RFC). */
	private boolean canViewPii() {
		return RolePolicy.canViewFullPii(SecurityContextHolder.getContext().getAuthentication());
	}

	public boolean piiMaskedForCurrentActor() {
		return !canViewPii();
	}

	private String fullName(Officer o) {
		StringBuilder sb = new StringBuilder(o.getNombre()).append(' ').append(o.getApellidoPaterno());
		if (o.getApellidoMaterno() != null && !o.getApellidoMaterno().isBlank()) {
			sb.append(' ').append(o.getApellidoMaterno());
		}
		return sb.toString();
	}

	private String safe(String value) {
		return value == null ? "" : value;
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value;
	}

	private String normalizeText(String value) {
		return (value == null || value.isBlank()) ? null : value.trim().toUpperCase(Locale.ROOT);
	}

	private String likeOrNull(String normalized) {
		return (normalized == null || normalized.isBlank()) ? null : "%" + normalized + "%";
	}

	private String likeUpperOrNull(String value) {
		String n = normalizeText(value);
		return n == null ? null : "%" + n + "%";
	}
}
