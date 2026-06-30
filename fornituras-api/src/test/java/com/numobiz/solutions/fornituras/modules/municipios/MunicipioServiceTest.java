package com.numobiz.solutions.fornituras.modules.municipios;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.municipios.dto.MunicipioCreateRequest;
import com.numobiz.solutions.fornituras.modules.municipios.entity.Municipio;
import com.numobiz.solutions.fornituras.modules.municipios.mapper.MunicipioMapper;
import com.numobiz.solutions.fornituras.modules.municipios.repository.MunicipioRepository;
import com.numobiz.solutions.fornituras.modules.municipios.service.MunicipioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MunicipioServiceTest {

	@Mock
	private MunicipioRepository repository;
	@Mock
	private MunicipioMapper mapper;
	@Mock
	private AuditWriter audit;

	@InjectMocks
	private MunicipioService service;

	@Test
	void create_rejectsDuplicateNormalizedName() {
		when(repository.existsByNombreNormalizado("guadalajara")).thenReturn(true);

		assertThrows(ConflictException.class,
				() -> service.create(new MunicipioCreateRequest("  Guadalajara ")));
		verify(repository, never()).save(any());
	}

	@Test
	void create_persistsTrimmedNameAndNormalizedKey() {
		when(repository.existsByNombreNormalizado("guadalajara")).thenReturn(false);
		when(repository.save(any(Municipio.class))).thenAnswer(i -> i.getArgument(0));

		service.create(new MunicipioCreateRequest("  Guadalajará "));

		ArgumentCaptor<Municipio> captor = ArgumentCaptor.forClass(Municipio.class);
		verify(repository).save(captor.capture());
		assertEquals("Guadalajará", captor.getValue().getNombre());
		assertEquals("guadalajara", captor.getValue().getNombreNormalizado());
		verify(audit).record("CREATE_MUNICIPIO", null);
	}
}
