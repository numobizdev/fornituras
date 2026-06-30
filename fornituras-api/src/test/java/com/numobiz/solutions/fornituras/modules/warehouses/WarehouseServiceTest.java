package com.numobiz.solutions.fornituras.modules.warehouses;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.municipios.repository.MunicipioRepository;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseCreateRequest;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.WarehouseType;
import com.numobiz.solutions.fornituras.modules.warehouses.mapper.WarehouseMapper;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.service.WarehouseService;
import com.numobiz.solutions.fornituras.modules.warehouses.service.WarehouseUsageQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

	@Mock
	private WarehouseRepository repository;
	@Mock
	private WarehouseMapper mapper;
	@Mock
	private WarehouseUsageQuery usageQuery;
	@Mock
	private MunicipioRepository municipioRepository;
	@Mock
	private UserRepository userRepository;
	@Mock
	private AuditWriter audit;

	@InjectMocks
	private WarehouseService service;

	@Test
	void create_rejectsDuplicateCodigo() {
		when(repository.existsByCodigoIgnoreCase("ALM-01")).thenReturn(true);

		assertThrows(ConflictException.class, () -> service.create(req("ALM-01", "Almacén Centro")));
		verify(repository, never()).save(any());
	}

	@Test
	void create_rejectsDuplicateNormalizedName() {
		when(repository.existsByCodigoIgnoreCase("ALM-02")).thenReturn(false);
		when(repository.existsByNombreNormalizado("almacen centro")).thenReturn(true);

		assertThrows(ConflictException.class, () -> service.create(req("ALM-02", "  Almacén  Centro ")));
		verify(repository, never()).save(any());
	}

	@Test
	void create_rejectsUnknownMunicipio() {
		when(repository.existsByCodigoIgnoreCase("ALM-03")).thenReturn(false);
		when(repository.existsByNombreNormalizado(any())).thenReturn(false);
		when(municipioRepository.existsById(99L)).thenReturn(false);

		WarehouseCreateRequest request = new WarehouseCreateRequest(
				"ALM-03", "Almacén Norte", WarehouseType.REGIONAL, 99L,
				null, null, null, null, null, null, null, null, null);

		assertThrows(BadRequestException.class, () -> service.create(request));
		verify(repository, never()).save(any());
	}

	@Test
	void create_persistsTrimmedCodigoAndNormalizedName() {
		when(repository.existsByCodigoIgnoreCase("ALM-04")).thenReturn(false);
		when(repository.existsByNombreNormalizado("almacen centro")).thenReturn(false);
		when(repository.save(any(Warehouse.class))).thenAnswer(i -> i.getArgument(0));

		service.create(req("  ALM-04 ", "  Almacén Centro "));

		ArgumentCaptor<Warehouse> captor = ArgumentCaptor.forClass(Warehouse.class);
		verify(repository).save(captor.capture());
		assertEquals("ALM-04", captor.getValue().getCodigo());
		assertEquals("Almacén Centro", captor.getValue().getNombre());
		assertEquals("almacen centro", captor.getValue().getNombreNormalizado());
		verify(audit).record("CREATE_WAREHOUSE", null);
	}

	@Test
	void deactivate_marksInactiveInsteadOfDeleting() {
		Warehouse warehouse = warehouseWithId(1L);
		when(repository.findById(1L)).thenReturn(Optional.of(warehouse));

		service.deactivate(1L);

		assertFalse(warehouse.isActive());
		verify(repository).save(warehouse);
		verify(repository, never()).delete(any());
		verify(audit).record("DEACTIVATE_WAREHOUSE", 1L);
	}

	@Test
	void delete_blockedWhenInUse() {
		Warehouse warehouse = warehouseWithId(1L);
		when(repository.findById(1L)).thenReturn(Optional.of(warehouse));
		when(usageQuery.countUsage(1L)).thenReturn(3L);

		assertThrows(ConflictException.class, () -> service.delete(1L));
		verify(repository, never()).delete(any());
	}

	@Test
	void delete_removesWhenNotInUse() {
		Warehouse warehouse = warehouseWithId(1L);
		when(repository.findById(1L)).thenReturn(Optional.of(warehouse));
		when(usageQuery.countUsage(1L)).thenReturn(0L);

		service.delete(1L);

		verify(repository).delete(warehouse);
		verify(audit).record("DELETE_WAREHOUSE", 1L);
	}

	private WarehouseCreateRequest req(String codigo, String nombre) {
		return new WarehouseCreateRequest(
				codigo, nombre, WarehouseType.CENTRAL, null,
				null, null, null, null, null, null, null, null, null);
	}

	private Warehouse warehouseWithId(Long id) {
		Warehouse warehouse = new Warehouse();
		warehouse.setId(id);
		warehouse.setCodigo("ALM-" + id);
		warehouse.setNombre("Almacén " + id);
		warehouse.setNombreNormalizado("almacen " + id);
		warehouse.setTipo(WarehouseType.CENTRAL);
		warehouse.setActive(true);
		return warehouse;
	}
}
