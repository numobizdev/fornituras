package com.numobiz.solutions.fornituras.modules.equipmenttypes;

import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.EquipmentType;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.EquipmentTypeMapper;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.SizeMapper;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.EquipmentTypeRepository;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.SizeRepository;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.service.CatalogAuditWriter;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.service.EquipmentTypeService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EquipmentTypeServiceTest {

	@Mock
	private EquipmentTypeRepository repository;
	@Mock
	private SizeRepository sizeRepository;
	@Mock
	private EquipmentTypeMapper mapper;
	@Mock
	private SizeMapper sizeMapper;
	@Mock
	private CatalogAuditWriter audit;

	@InjectMocks
	private EquipmentTypeService service;

	@Test
	void create_rejectsDuplicateNormalizedName() {
		when(repository.existsByNombreNormalizado("chaleco antibala")).thenReturn(true);

		assertThrows(ConflictException.class,
				() -> service.create(new EquipmentTypeCreateRequest("  Chaleco  Antibalá ", null, null)));
		verify(repository, never()).save(any());
	}

	@Test
	void create_persistsTrimmedNameAndNormalizedKey() {
		when(repository.existsByNombreNormalizado("chaleco antibala")).thenReturn(false);
		when(repository.save(any(EquipmentType.class))).thenAnswer(invocation -> invocation.getArgument(0));
		lenient().when(mapper.toDetail(any(), any())).thenReturn(null);

		service.create(new EquipmentTypeCreateRequest("  Chaleco Antibalá ", "desc", null));

		ArgumentCaptor<EquipmentType> captor = ArgumentCaptor.forClass(EquipmentType.class);
		verify(repository).save(captor.capture());
		assertEquals("Chaleco Antibalá", captor.getValue().getNombre());
		assertEquals("chaleco antibala", captor.getValue().getNombreNormalizado());
		verify(audit).record("CREATE_EQUIPMENT_TYPE", null);
	}

	@Test
	void update_rejectsNameOwnedByAnotherType() {
		EquipmentType current = typeWithId(1L, "Casco");
		EquipmentType other = typeWithId(2L, "Chaleco");
		when(repository.findById(1L)).thenReturn(Optional.of(current));
		when(repository.findByNombreNormalizado("chaleco")).thenReturn(Optional.of(other));

		assertThrows(ConflictException.class,
				() -> service.update(1L, new EquipmentTypeCreateRequest("Chaleco", null, null)));
	}

	@Test
	void deactivate_marksInactiveInsteadOfDeleting() {
		EquipmentType type = typeWithId(1L, "Casco");
		when(repository.findById(1L)).thenReturn(Optional.of(type));

		service.deactivate(1L);

		assertFalse(type.isActive());
		verify(repository).save(type);
		verify(repository, never()).delete(any());
		verify(audit).record("DEACTIVATE_EQUIPMENT_TYPE", 1L);
	}

	private EquipmentType typeWithId(Long id, String nombre) {
		EquipmentType type = new EquipmentType();
		type.setId(id);
		type.setNombre(nombre);
		type.setNombreNormalizado(nombre.toLowerCase());
		type.setActive(true);
		return type;
	}
}
