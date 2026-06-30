package com.numobiz.solutions.fornituras.modules.assignments.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.assignments.dto.AssignRequest;
import com.numobiz.solutions.fornituras.modules.assignments.dto.AssignmentSummary;
import com.numobiz.solutions.fornituras.modules.assignments.dto.ReassignRequest;
import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.repository.OfficerRepository;
import com.numobiz.solutions.fornituras.modules.users.entity.User;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Núcleo de SIGEFOR: liga fornituras (001) con elementos (003) en el tiempo. Garantiza que una
 * fornitura solo se asigna si está <b>disponible</b> y que nunca tiene dos asignaciones vigentes
 * (lo refuerza el índice único filtrado en BD; un conflicto de concurrencia se traduce a 409).
 * Cada operación cambia el estado de la fornitura y queda auditada (sin PII; referencias por id).
 */
@Service
@Transactional(readOnly = true)
public class AssignmentService {

	private final AssignmentRepository repository;
	private final EquipmentRepository equipmentRepository;
	private final OfficerRepository officerRepository;
	private final UserRepository userRepository;
	private final AuditWriter audit;

	public AssignmentService(
			AssignmentRepository repository,
			EquipmentRepository equipmentRepository,
			OfficerRepository officerRepository,
			UserRepository userRepository,
			AuditWriter audit) {
		this.repository = repository;
		this.equipmentRepository = equipmentRepository;
		this.officerRepository = officerRepository;
		this.userRepository = userRepository;
		this.audit = audit;
	}

	public Page<AssignmentSummary> findVigentes(Pageable pageable) {
		return repository.findByFechaDevolucionIsNull(pageable).map(this::toSummary);
	}

	@Transactional
	public AssignmentSummary assign(AssignRequest request) {
		Equipment equipment = equipmentRepository.findById(request.equipmentId())
				.orElseThrow(() -> new NotFoundException("Fornitura no encontrada: " + request.equipmentId()));
		if (equipment.getStatus() != EquipmentStatus.DISPONIBLE) {
			throw new ConflictException("La fornitura no está disponible (estado actual: " + equipment.getStatus() + ").");
		}
		Officer officer = requireActiveOfficer(request.officerId());
		if (repository.existsByEquipmentIdAndFechaDevolucionIsNull(request.equipmentId())) {
			throw new ConflictException("La fornitura ya tiene una asignación vigente.");
		}

		Assignment assignment = new Assignment();
		assignment.setEquipmentId(request.equipmentId());
		assignment.setOfficerId(request.officerId());
		assignment.setFechaAsignacion(LocalDateTime.now());
		assignment.setAsignadoPor(currentUserId());
		assignment.setObservaciones(request.observaciones());

		Assignment saved = persistVigente(assignment);
		markEquipmentStatus(equipment, EquipmentStatus.ASIGNADA);
		audit.record("ASSIGN", saved.getId());
		return toSummary(saved, equipment, officer);
	}

	@Transactional
	public AssignmentSummary returnAssignment(Long id) {
		Assignment assignment = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Asignación no encontrada: " + id));
		if (assignment.getFechaDevolucion() != null) {
			throw new ConflictException("La asignación ya fue devuelta.");
		}
		assignment.setFechaDevolucion(LocalDateTime.now());
		assignment.setRecibidoPor(currentUserId());
		Assignment saved = repository.save(assignment);

		equipmentRepository.findById(assignment.getEquipmentId())
				.ifPresent(equipment -> markEquipmentStatus(equipment, EquipmentStatus.DISPONIBLE));
		audit.record("RETURN", id);
		return toSummary(saved);
	}

	@Transactional
	public AssignmentSummary reassign(ReassignRequest request) {
		Assignment current = repository.findByEquipmentIdAndFechaDevolucionIsNull(request.equipmentId())
				.orElseThrow(() -> new ConflictException("La fornitura no tiene una asignación vigente para reasignar."));
		requireActiveOfficer(request.newOfficerId());

		current.setFechaDevolucion(LocalDateTime.now());
		current.setRecibidoPor(currentUserId());
		// Cierra la vigente y descarga el cambio para liberar el índice único filtrado antes de abrir la nueva.
		repository.saveAndFlush(current);

		Assignment next = new Assignment();
		next.setEquipmentId(request.equipmentId());
		next.setOfficerId(request.newOfficerId());
		next.setFechaAsignacion(LocalDateTime.now());
		next.setAsignadoPor(currentUserId());
		next.setObservaciones(request.observaciones());

		Assignment saved = persistVigente(next);
		// La fornitura sigue "asignada"; no cambia de estado en una reasignación.
		audit.record("REASSIGN", saved.getId());
		return toSummary(saved);
	}

	private Officer requireActiveOfficer(Long officerId) {
		Officer officer = officerRepository.findById(officerId)
				.orElseThrow(() -> new NotFoundException("Elemento no encontrado: " + officerId));
		if (!officer.isActive()) {
			throw new BadRequestException("El elemento está inactivo.");
		}
		return officer;
	}

	/** Persiste una asignación vigente traduciendo la colisión del índice único filtrado a 409. */
	private Assignment persistVigente(Assignment assignment) {
		try {
			return repository.saveAndFlush(assignment);
		} catch (DataIntegrityViolationException e) {
			throw new ConflictException("La fornitura ya tiene una asignación vigente.");
		}
	}

	private void markEquipmentStatus(Equipment equipment, EquipmentStatus status) {
		equipment.setStatus(status);
		equipmentRepository.save(equipment);
	}

	private Long currentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getName() == null) {
			return null;
		}
		return userRepository.findByEmail(auth.getName()).map(User::getId).orElse(null);
	}

	private AssignmentSummary toSummary(Assignment assignment) {
		Equipment equipment = equipmentRepository.findById(assignment.getEquipmentId()).orElse(null);
		Officer officer = officerRepository.findById(assignment.getOfficerId()).orElse(null);
		return toSummary(assignment, equipment, officer);
	}

	private AssignmentSummary toSummary(Assignment assignment, Equipment equipment, Officer officer) {
		return new AssignmentSummary(
				assignment.getId(),
				assignment.getEquipmentId(),
				equipment == null ? null : equipment.getCodigoQr(),
				equipment == null ? null : equipment.getDescripcion(),
				assignment.getOfficerId(),
				officer == null ? null : fullName(officer),
				officer == null ? null : officer.getPlaca(),
				assignment.getFechaAsignacion(),
				assignment.getFechaDevolucion(),
				assignment.getFechaDevolucion() == null);
	}

	private String fullName(Officer officer) {
		StringBuilder sb = new StringBuilder(officer.getNombre()).append(' ').append(officer.getApellidoPaterno());
		if (officer.getApellidoMaterno() != null && !officer.getApellidoMaterno().isBlank()) {
			sb.append(' ').append(officer.getApellidoMaterno());
		}
		return sb.toString();
	}
}
