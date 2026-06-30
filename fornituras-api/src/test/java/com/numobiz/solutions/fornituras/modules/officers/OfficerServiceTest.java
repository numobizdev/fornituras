package com.numobiz.solutions.fornituras.modules.officers;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.crypto.BlindIndexer;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.municipios.entity.Municipio;
import com.numobiz.solutions.fornituras.modules.municipios.repository.MunicipioRepository;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerCreateRequest;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.entity.Sexo;
import com.numobiz.solutions.fornituras.modules.officers.mapper.OfficerMapper;
import com.numobiz.solutions.fornituras.modules.officers.repository.OfficerRepository;
import com.numobiz.solutions.fornituras.modules.officers.repository.SexoRepository;
import com.numobiz.solutions.fornituras.modules.officers.repository.TipoSangreRepository;
import com.numobiz.solutions.fornituras.modules.officers.service.OfficerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfficerServiceTest {

	@Mock
	private OfficerRepository repository;
	@Mock
	private OfficerMapper mapper;
	@Mock
	private BlindIndexer blindIndexer;
	@Mock
	private SexoRepository sexoRepository;
	@Mock
	private TipoSangreRepository tipoSangreRepository;
	@Mock
	private MunicipioRepository municipioRepository;
	@Mock
	private AuditWriter audit;

	@InjectMocks
	private OfficerService service;

	@Test
	void create_rejectsDuplicatePlaca() {
		when(repository.existsByPlacaNormalizada("ABC123")).thenReturn(true);

		assertThrows(ConflictException.class, () -> service.create(req("ABC-123", null)));
		verify(repository, never()).save(any());
	}

	@Test
	void create_rejectsDuplicateCurp() {
		when(repository.existsByPlacaNormalizada("ABC123")).thenReturn(false);
		when(blindIndexer.index("CURP800101HDFXYZ01")).thenReturn("curphash");
		when(repository.existsByCurpIdx("curphash")).thenReturn(true);

		assertThrows(ConflictException.class, () -> service.create(req("ABC-123", "CURP800101HDFXYZ01")));
		verify(repository, never()).save(any());
	}

	@Test
	void create_persistsNormalizedPlacaAndAudits() {
		when(repository.existsByPlacaNormalizada("ABC123")).thenReturn(false);
		when(sexoRepository.findById(1L)).thenReturn(Optional.of(sexo()));
		when(municipioRepository.findById(2L)).thenReturn(Optional.of(municipio()));
		when(repository.save(any(Officer.class))).thenAnswer(i -> i.getArgument(0));

		service.create(req("  abc-123 ", null));

		ArgumentCaptor<Officer> captor = ArgumentCaptor.forClass(Officer.class);
		verify(repository).save(captor.capture());
		assertEquals("ABC-123", captor.getValue().getPlaca());
		assertEquals("ABC123", captor.getValue().getPlacaNormalizada());
		verify(audit).record("CREATE_OFFICER", null);
	}

	@Test
	void findById_auditsAccess() {
		Officer officer = officer();
		when(repository.findById(1L)).thenReturn(Optional.of(officer));

		service.findById(1L);

		verify(audit).record("VIEW_OFFICER", 1L);
	}

	private OfficerCreateRequest req(String placa, String curp) {
		return new OfficerCreateRequest("Juan", "Pérez", null, placa, 1L, null, 2L, curp, null, null);
	}

	private Sexo sexo() {
		Sexo sexo = new Sexo();
		sexo.setId(1L);
		sexo.setNombre("MASCULINO");
		sexo.setActive(true);
		return sexo;
	}

	private Municipio municipio() {
		Municipio municipio = new Municipio();
		municipio.setId(2L);
		municipio.setActive(true);
		return municipio;
	}

	private Officer officer() {
		Officer officer = new Officer();
		officer.setId(1L);
		officer.setNombre("Juan");
		officer.setApellidoPaterno("Pérez");
		officer.setPlaca("ABC-123");
		officer.setPlacaNormalizada("ABC123");
		officer.setSexoId(1L);
		officer.setMunicipioId(2L);
		officer.setActive(true);
		return officer;
	}
}
