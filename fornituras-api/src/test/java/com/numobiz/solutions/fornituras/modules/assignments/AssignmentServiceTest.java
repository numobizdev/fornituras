package com.numobiz.solutions.fornituras.modules.assignments;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.assignments.dto.AssignRequest;
import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.assignments.service.AssignmentService;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.repository.OfficerRepository;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

	@Mock
	private AssignmentRepository repository;
	@Mock
	private EquipmentRepository equipmentRepository;
	@Mock
	private OfficerRepository officerRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private AuditWriter audit;

	@InjectMocks
	private AssignmentService service;

	@Test
	void assign_rejectsWhenEquipmentNotAvailable() {
		when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipment(EquipmentStatus.ASIGNADA)));

		assertThrows(ConflictException.class, () -> service.assign(new AssignRequest(10L, 20L, null)));
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void assign_rejectsWhenAlreadyHasActiveAssignment() {
		when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipment(EquipmentStatus.DISPONIBLE)));
		when(officerRepository.findById(20L)).thenReturn(Optional.of(officer()));
		when(repository.existsByEquipmentIdAndFechaDevolucionIsNull(10L)).thenReturn(true);

		assertThrows(ConflictException.class, () -> service.assign(new AssignRequest(10L, 20L, null)));
		verify(repository, never()).saveAndFlush(any());
	}

	@Test
	void assign_createsAndMarksEquipmentAssigned() {
		Equipment equipment = equipment(EquipmentStatus.DISPONIBLE);
		when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipment));
		when(officerRepository.findById(20L)).thenReturn(Optional.of(officer()));
		when(repository.existsByEquipmentIdAndFechaDevolucionIsNull(10L)).thenReturn(false);
		when(repository.saveAndFlush(any(Assignment.class))).thenAnswer(i -> i.getArgument(0));

		service.assign(new AssignRequest(10L, 20L, "entrega"));

		ArgumentCaptor<Assignment> captor = ArgumentCaptor.forClass(Assignment.class);
		verify(repository).saveAndFlush(captor.capture());
		assertEquals(10L, captor.getValue().getEquipmentId());
		assertEquals(20L, captor.getValue().getOfficerId());
		assertNotNull(captor.getValue().getFechaAsignacion());
		assertEquals(EquipmentStatus.ASIGNADA, equipment.getStatus());
		verify(equipmentRepository).save(equipment);
		verify(audit).record("ASSIGN", null);
	}

	@Test
	void returnAssignment_freesEquipment() {
		Assignment assignment = vigente();
		Equipment equipment = equipment(EquipmentStatus.ASIGNADA);
		when(repository.findById(1L)).thenReturn(Optional.of(assignment));
		when(repository.save(assignment)).thenAnswer(i -> i.getArgument(0));
		when(equipmentRepository.findById(10L)).thenReturn(Optional.of(equipment));
		when(officerRepository.findById(20L)).thenReturn(Optional.of(officer()));

		service.returnAssignment(1L);

		assertNotNull(assignment.getFechaDevolucion());
		assertEquals(EquipmentStatus.DISPONIBLE, equipment.getStatus());
		verify(audit).record("RETURN", 1L);
	}

	@Test
	void returnAssignment_rejectsWhenAlreadyReturned() {
		Assignment assignment = vigente();
		assignment.setFechaDevolucion(LocalDateTime.now());
		when(repository.findById(1L)).thenReturn(Optional.of(assignment));

		assertThrows(ConflictException.class, () -> service.returnAssignment(1L));
		verify(repository, never()).save(any());
	}

	private Equipment equipment(EquipmentStatus status) {
		Equipment equipment = new Equipment();
		equipment.setId(10L);
		equipment.setCodigoQr("FOR-1");
		equipment.setDescripcion("Chaleco");
		equipment.setStatus(status);
		return equipment;
	}

	private Officer officer() {
		Officer officer = new Officer();
		officer.setId(20L);
		officer.setNombre("Juan");
		officer.setApellidoPaterno("Pérez");
		officer.setPlaca("ABC-1");
		officer.setActive(true);
		return officer;
	}

	private Assignment vigente() {
		Assignment assignment = new Assignment();
		assignment.setId(1L);
		assignment.setEquipmentId(10L);
		assignment.setOfficerId(20L);
		assignment.setFechaAsignacion(LocalDateTime.now());
		return assignment;
	}
}
