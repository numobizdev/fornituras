package com.numobiz.solutions.fornituras.modules.incidents.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentService;
import com.numobiz.solutions.fornituras.modules.incidents.dto.IncidentCreateRequest;
import com.numobiz.solutions.fornituras.modules.incidents.dto.IncidentSummary;
import com.numobiz.solutions.fornituras.modules.incidents.dto.IncidentUpdateRequest;
import com.numobiz.solutions.fornituras.modules.incidents.entity.Incident;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentType;
import com.numobiz.solutions.fornituras.modules.incidents.repository.IncidentRepository;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seguimiento de incidencias sobre fornituras. Reportar una incidencia la deja "abierta" y, salvo que
 * la fornitura esté bajo una custodia activa (en traslado), la retira ("en mantenimiento" o
 * "extraviada"); resolverla/cerrarla la devuelve a "disponible" si seguía retirada por la incidencia.
 * El cambio de estado se delega en {@link EquipmentService} (única fuente de las transiciones, 001) y
 * toda mutación queda auditada por id (sin PII).
 */
@Service
@Transactional(readOnly = true)
public class IncidentService {

	private final IncidentRepository incidentRepository;
	private final EquipmentRepository equipmentRepository;
	private final EquipmentService equipmentService;
	private final UserRepository userRepository;
	private final AuditWriter audit;

	public IncidentService(
			IncidentRepository incidentRepository,
			EquipmentRepository equipmentRepository,
			EquipmentService equipmentService,
			UserRepository userRepository,
			AuditWriter audit) {
		this.incidentRepository = incidentRepository;
		this.equipmentRepository = equipmentRepository;
		this.equipmentService = equipmentService;
		this.userRepository = userRepository;
		this.audit = audit;
	}

	public Page<IncidentSummary> findAll(IncidentStatus estado, Pageable pageable) {
		Page<Incident> page = (estado == null)
				? incidentRepository.findAll(pageable)
				: incidentRepository.findByEstado(estado, pageable);
		Map<Long, String> codigos = resolveCodigos(page.map(Incident::getEquipmentId).getContent());
		return page.map(incident -> toSummary(incident, codigos.get(incident.getEquipmentId())));
	}

	public IncidentSummary findById(Long id) {
		Incident incident = getOrThrow(id);
		return toSummary(incident, codigoOf(incident.getEquipmentId()));
	}

	@Transactional
	public IncidentSummary report(IncidentCreateRequest request) {
		Equipment equipment = equipmentRepository.findById(request.equipmentId())
				.orElseThrow(() -> new NotFoundException("Fornitura no encontrada: " + request.equipmentId()));

		Incident incident = new Incident();
		incident.setEquipmentId(request.equipmentId());
		incident.setTipo(request.tipo());
		incident.setDescripcion(request.descripcion().trim());
		incident.setEstado(IncidentStatus.ABIERTA);
		incident.setFechaReporte(LocalDateTime.now());
		incident.setReportadoPor(currentUserId());
		Incident saved = incidentRepository.save(incident);

		retireEquipmentIfApplicable(equipment, request.tipo());

		audit.record("REPORT_INCIDENT", saved.getId());
		return toSummary(saved, equipment.getCodigoQr());
	}

	@Transactional
	public IncidentSummary update(Long id, IncidentUpdateRequest request) {
		Incident incident = getOrThrow(id);
		incident.setEstado(request.estado());
		incident.setActualizadoPor(currentUserId());

		if (isClosing(request.estado())) {
			if (incident.getFechaResolucion() == null) {
				incident.setFechaResolucion(LocalDateTime.now());
			}
			releaseEquipmentIfRetired(incident.getEquipmentId());
		}

		Incident saved = incidentRepository.save(incident);
		audit.record("UPDATE_INCIDENT", id);
		return toSummary(saved, codigoOf(saved.getEquipmentId()));
	}

	/**
	 * Retira la fornitura según el tipo (extravío → extraviada; resto → en mantenimiento), pero solo si
	 * está disponible o asignada: no se interfiere con una custodia activa (en traslado) ni con bajas.
	 */
	private void retireEquipmentIfApplicable(Equipment equipment, IncidentType tipo) {
		EquipmentStatus current = equipment.getStatus();
		if (current != EquipmentStatus.DISPONIBLE && current != EquipmentStatus.ASIGNADA) {
			return;
		}
		EquipmentStatus target = (tipo == IncidentType.EXTRAVIO)
				? EquipmentStatus.EXTRAVIADA
				: EquipmentStatus.EN_MANTENIMIENTO;
		equipmentService.changeStatus(equipment.getId(), target);
	}

	/** Al resolver/cerrar, devuelve la fornitura a disponible solo si seguía retirada por la incidencia. */
	private void releaseEquipmentIfRetired(Long equipmentId) {
		equipmentRepository.findById(equipmentId).ifPresent(equipment -> {
			EquipmentStatus current = equipment.getStatus();
			if (current == EquipmentStatus.EN_MANTENIMIENTO || current == EquipmentStatus.EXTRAVIADA) {
				equipmentService.changeStatus(equipmentId, EquipmentStatus.DISPONIBLE);
			}
		});
	}

	private boolean isClosing(IncidentStatus estado) {
		return estado == IncidentStatus.RESUELTA || estado == IncidentStatus.CERRADA;
	}

	private Incident getOrThrow(Long id) {
		return incidentRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Incidencia no encontrada: " + id));
	}

	private String codigoOf(Long equipmentId) {
		return equipmentRepository.findById(equipmentId).map(Equipment::getCodigoQr).orElse(null);
	}

	private Map<Long, String> resolveCodigos(Collection<Long> equipmentIds) {
		if (equipmentIds.isEmpty()) {
			return Map.of();
		}
		return equipmentRepository.findAllById(equipmentIds).stream()
				.collect(Collectors.toMap(Equipment::getId, Equipment::getCodigoQr));
	}

	private Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			return null;
		}
		return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
	}

	private IncidentSummary toSummary(Incident incident, String equipmentCodigo) {
		return new IncidentSummary(
				incident.getId(),
				incident.getEquipmentId(),
				equipmentCodigo,
				incident.getTipo(),
				incident.getDescripcion(),
				incident.getEstado(),
				incident.getFechaReporte(),
				incident.getFechaResolucion());
	}
}
