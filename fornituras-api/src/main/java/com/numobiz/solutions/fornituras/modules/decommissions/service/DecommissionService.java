package com.numobiz.solutions.fornituras.modules.decommissions.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.catalog.service.CatalogService;
import com.numobiz.solutions.fornituras.modules.decommissions.dto.DecommissionReasonItem;
import com.numobiz.solutions.fornituras.modules.decommissions.dto.DecommissionRequest;
import com.numobiz.solutions.fornituras.modules.decommissions.dto.DecommissionSummary;
import com.numobiz.solutions.fornituras.modules.decommissions.entity.Decommission;
import com.numobiz.solutions.fornituras.modules.decommissions.entity.DecommissionReason;
import com.numobiz.solutions.fornituras.modules.decommissions.repository.DecommissionReasonRepository;
import com.numobiz.solutions.fornituras.modules.decommissions.repository.DecommissionRepository;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentDetail;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentService;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Baja definitiva de fornituras. Resuelve la fornitura por código (server-side, 001), valida el motivo
 * contra el catálogo y delega el cambio de estado a {@link EquipmentService} —única fuente de las
 * transiciones—, que bloquea la baja si la fornitura tiene asignación vigente o traslado en curso
 * (FR-002). Registra la baja (motivo, fecha, responsable) preservando el historial de la fornitura y
 * la audita por id (FR-005). Por política, una baja no se revierte.
 */
@Service
@Transactional(readOnly = true)
public class DecommissionService {

	private final DecommissionRepository repository;
	private final DecommissionReasonRepository reasonRepository;
	private final EquipmentService equipmentService;
	private final EquipmentRepository equipmentRepository;
	private final CatalogService catalogService;
	private final UserRepository userRepository;
	private final AuditWriter audit;

	public DecommissionService(
			DecommissionRepository repository,
			DecommissionReasonRepository reasonRepository,
			EquipmentService equipmentService,
			EquipmentRepository equipmentRepository,
			CatalogService catalogService,
			UserRepository userRepository,
			AuditWriter audit) {
		this.repository = repository;
		this.reasonRepository = reasonRepository;
		this.equipmentService = equipmentService;
		this.equipmentRepository = equipmentRepository;
		this.catalogService = catalogService;
		this.userRepository = userRepository;
		this.audit = audit;
	}

	public List<DecommissionReasonItem> findReasons() {
		return reasonRepository.findByActiveTrueOrderByNombre().stream()
				.map(reason -> new DecommissionReasonItem(reason.getId(), reason.getNombre()))
				.toList();
	}

	@Transactional
	public DecommissionSummary decommission(DecommissionRequest request) {
		EquipmentDetail equipment = equipmentService.findByCodigo(request.codigo());
		if (equipment.status() == EquipmentStatus.BAJA_DEFINITIVA) {
			throw new ConflictException("La fornitura ya está dada de baja.");
		}
		DecommissionReason reason = requireActiveReason(request.motivoId());

		// Bloquea si hay asignación vigente/traslado en curso y persiste el estado "baja definitiva" (001).
		equipmentService.changeStatus(equipment.id(), EquipmentStatus.BAJA_DEFINITIVA);

		Decommission decommission = new Decommission();
		decommission.setEquipmentId(equipment.id());
		decommission.setMotivoId(reason.getId());
		decommission.setFecha(LocalDate.now());
		decommission.setResponsable(currentUserId());
		decommission.setObservaciones(trimToNull(request.observaciones()));
		Decommission saved = repository.save(decommission);

		audit.record("DECOMMISSION_EQUIPMENT", saved.getId());
		return toSummary(saved, equipment.codigoQr(), equipment.descripcion(), equipment.tipoNombre(),
				reason.getNombre());
	}

	public Page<DecommissionSummary> findAll(
			LocalDate fechaDesde, LocalDate fechaHasta, Long equipmentTypeId, Long motivoId, Pageable pageable) {
		Page<Decommission> page = repository.findAll(
				filterBy(fechaDesde, fechaHasta, equipmentTypeId, motivoId), pageable);
		List<Decommission> content = page.getContent();

		Map<Long, Equipment> equipments = equipmentRepository.findAllById(
						content.stream().map(Decommission::getEquipmentId).collect(Collectors.toSet())).stream()
				.collect(Collectors.toMap(Equipment::getId, e -> e));
		Map<Long, String> typeNames = catalogService.resolveNames(equipments.values().stream()
				.map(Equipment::getEquipmentTypeId).collect(Collectors.toSet()));
		Map<Long, String> reasonNames = resolveReasonNames(
				content.stream().map(Decommission::getMotivoId).collect(Collectors.toSet()));

		return page.map(decommission -> {
			Equipment equipment = equipments.get(decommission.getEquipmentId());
			String codigo = equipment == null ? null : equipment.getCodigoQr();
			String descripcion = equipment == null ? null : equipment.getDescripcion();
			String tipoNombre = equipment == null ? null : typeNames.get(equipment.getEquipmentTypeId());
			return toSummary(decommission, codigo, descripcion, tipoNombre,
					reasonNames.get(decommission.getMotivoId()));
		});
	}

	private DecommissionReason requireActiveReason(Long motivoId) {
		DecommissionReason reason = reasonRepository.findById(motivoId)
				.orElseThrow(() -> new BadRequestException("Motivo de baja no encontrado: " + motivoId));
		if (!reason.isActive()) {
			throw new BadRequestException("El motivo de baja está inactivo.");
		}
		return reason;
	}

	private Specification<Decommission> filterBy(
			LocalDate fechaDesde, LocalDate fechaHasta, Long equipmentTypeId, Long motivoId) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (fechaDesde != null) {
				predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
			}
			if (fechaHasta != null) {
				predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
			}
			if (motivoId != null) {
				predicates.add(cb.equal(root.get("motivoId"), motivoId));
			}
			if (equipmentTypeId != null) {
				List<Long> equipmentIds = equipmentRepository.findIdsByEquipmentTypeId(equipmentTypeId);
				predicates.add(equipmentIds.isEmpty()
						? cb.disjunction()
						: root.get("equipmentId").in(equipmentIds));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private Map<Long, String> resolveReasonNames(Collection<Long> motivoIds) {
		if (motivoIds.isEmpty()) {
			return Map.of();
		}
		return reasonRepository.findAllById(motivoIds).stream()
				.collect(Collectors.toMap(DecommissionReason::getId, DecommissionReason::getNombre));
	}

	private Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			return null;
		}
		return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
	}

	private String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private DecommissionSummary toSummary(
			Decommission decommission, String equipmentCodigo, String descripcion, String tipoNombre,
			String motivoNombre) {
		return new DecommissionSummary(
				decommission.getId(),
				decommission.getEquipmentId(),
				equipmentCodigo,
				descripcion,
				tipoNombre,
				decommission.getMotivoId(),
				motivoNombre,
				decommission.getFecha(),
				decommission.getResponsable(),
				decommission.getObservaciones());
	}
}
