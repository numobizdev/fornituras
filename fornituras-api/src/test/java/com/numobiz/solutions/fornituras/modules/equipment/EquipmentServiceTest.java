package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.equipment.dto.BatchCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.mapper.EquipmentMapper;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentLifecycleQuery;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentService;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.EquipmentType;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.EquipmentTypeRepository;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.SizeRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

	@Mock
	private EquipmentRepository repository;
	@Mock
	private EquipmentMapper mapper;
	@Mock
	private EquipmentTypeRepository equipmentTypeRepository;
	@Mock
	private SizeRepository sizeRepository;
	@Mock
	private WarehouseRepository warehouseRepository;
	@Mock
	private EquipmentLifecycleQuery lifecycle;
	@Mock
	private AuditWriter audit;

	@InjectMocks
	private EquipmentService service;

	@Test
	void create_rejectsDuplicateNormalizedCode() {
		when(repository.existsByCodigoNormalizado("FOR001")).thenReturn(true);

		assertThrows(ConflictException.class, () -> service.create(req("FOR-001")));
		verify(repository, never()).save(any());
	}

	@Test
	void create_rejectsInactiveType() {
		when(repository.existsByCodigoNormalizado("FOR002")).thenReturn(false);
		when(equipmentTypeRepository.findById(10L)).thenReturn(Optional.of(type(10L, false)));

		assertThrows(BadRequestException.class, () -> service.create(req("FOR-002")));
		verify(repository, never()).save(any());
	}

	@Test
	void create_persistsTrimmedDisplayAndNormalizedCode() {
		stubActiveCatalogs();
		when(repository.existsByCodigoNormalizado("FOR003")).thenReturn(false);
		when(repository.save(any(Equipment.class))).thenAnswer(i -> i.getArgument(0));

		service.create(req("  for-003 "));

		ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
		verify(repository).save(captor.capture());
		assertEquals("FOR-003", captor.getValue().getCodigoQr());
		assertEquals("FOR003", captor.getValue().getCodigoNormalizado());
		assertEquals(EquipmentStatus.DISPONIBLE, captor.getValue().getStatus());
		verify(audit).record("CREATE_EQUIPMENT", null);
	}

	@Test
	void create_derivesExpiryFromFabricationPlusMonths() {
		stubActiveCatalogs();
		when(repository.existsByCodigoNormalizado("FOR004")).thenReturn(false);
		when(repository.save(any(Equipment.class))).thenAnswer(i -> i.getArgument(0));

		EquipmentCreateRequest request = new EquipmentCreateRequest(
				"FOR-004", 10L, null, 20L, "desc", null, null, null, null,
				LocalDate.of(2020, 1, 15), null, 24, null, null, null);

		service.create(request);

		ArgumentCaptor<Equipment> captor = ArgumentCaptor.forClass(Equipment.class);
		verify(repository).save(captor.capture());
		assertEquals(LocalDate.of(2022, 1, 15), captor.getValue().getFechaVencimiento());
	}

	@Test
	void batch_rejectsDuplicateWithinLot() {
		stubActiveCatalogs();
		when(repository.existsByCodigoNormalizado("FOR1")).thenReturn(false);

		assertThrows(ConflictException.class,
				() -> service.createBatch(batch(List.of("FOR-1", "FOR1"))));
		verify(repository, never()).saveAll(any());
	}

	@Test
	void batch_rejectsCodeExistingInDatabase() {
		stubActiveCatalogs();
		when(repository.existsByCodigoNormalizado("FOR9")).thenReturn(true);

		assertThrows(ConflictException.class,
				() -> service.createBatch(batch(List.of("FOR-9"))));
		verify(repository, never()).saveAll(any());
	}

	@Test
	void batch_createsOnePerCodeAndAudits() {
		stubActiveCatalogs();
		when(repository.existsByCodigoNormalizado("FOR1")).thenReturn(false);
		when(repository.existsByCodigoNormalizado("FOR2")).thenReturn(false);
		when(repository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

		List<?> created = service.createBatch(batch(List.of("FOR-1", "FOR-2")));

		assertEquals(2, created.size());
		verify(audit, times(2)).record(any(), any());
	}

	@Test
	void update_rejectsCodeChange() {
		Equipment existing = equipmentWith("FOR1");
		when(repository.findById(1L)).thenReturn(Optional.of(existing));

		assertThrows(ConflictException.class, () -> service.update(1L, req("FOR-2")));
		verify(repository, never()).save(any());
	}

	@Test
	void changeStatus_blockedWhenAssignmentActive() {
		Equipment existing = equipmentWith("FOR1");
		when(repository.findById(1L)).thenReturn(Optional.of(existing));
		when(lifecycle.hasActiveAssignment(1L)).thenReturn(true);

		assertThrows(ConflictException.class,
				() -> service.changeStatus(1L, EquipmentStatus.BAJA_DEFINITIVA));
		verify(repository, never()).save(any());
	}

	@Test
	void changeStatus_appliesWhenAllowed() {
		Equipment existing = equipmentWith("FOR1");
		when(repository.findById(1L)).thenReturn(Optional.of(existing));
		when(repository.save(any(Equipment.class))).thenAnswer(i -> i.getArgument(0));

		service.changeStatus(1L, EquipmentStatus.EN_MANTENIMIENTO);

		assertEquals(EquipmentStatus.EN_MANTENIMIENTO, existing.getStatus());
		verify(audit).record("STATUS_CHANGE_EQUIPMENT", 1L);
	}

	private void stubActiveCatalogs() {
		when(equipmentTypeRepository.findById(10L)).thenReturn(Optional.of(type(10L, true)));
		when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse(20L)));
	}

	private EquipmentCreateRequest req(String codigo) {
		return new EquipmentCreateRequest(
				codigo, 10L, null, 20L, "desc", null, null, null, null,
				null, null, null, null, null, null);
	}

	private BatchCreateRequest batch(List<String> codigos) {
		return new BatchCreateRequest(
				10L, null, 20L, "desc", null, null, null,
				null, null, null, null, null, codigos);
	}

	private EquipmentType type(Long id, boolean active) {
		EquipmentType type = new EquipmentType();
		type.setId(id);
		type.setNombre("Chaleco");
		type.setActive(active);
		return type;
	}

	private Warehouse warehouse(Long id) {
		Warehouse warehouse = new Warehouse();
		warehouse.setId(id);
		warehouse.setNombre("Almacén Central");
		warehouse.setActive(true);
		return warehouse;
	}

	private Equipment equipmentWith(String normalized) {
		Equipment equipment = new Equipment();
		equipment.setId(1L);
		equipment.setCodigoQr(normalized);
		equipment.setCodigoNormalizado(normalized);
		equipment.setEquipmentTypeId(10L);
		equipment.setWarehouseId(20L);
		equipment.setStatus(EquipmentStatus.DISPONIBLE);
		return equipment;
	}
}
