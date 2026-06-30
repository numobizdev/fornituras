package com.numobiz.solutions.fornituras.modules.municipios.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.text.NameNormalizer;
import com.numobiz.solutions.fornituras.modules.municipios.dto.MunicipioCreateRequest;
import com.numobiz.solutions.fornituras.modules.municipios.dto.MunicipioSummary;
import com.numobiz.solutions.fornituras.modules.municipios.entity.Municipio;
import com.numobiz.solutions.fornituras.modules.municipios.mapper.MunicipioMapper;
import com.numobiz.solutions.fornituras.modules.municipios.repository.MunicipioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MunicipioService {

	private final MunicipioRepository repository;
	private final MunicipioMapper mapper;
	private final AuditWriter audit;

	public MunicipioService(MunicipioRepository repository, MunicipioMapper mapper, AuditWriter audit) {
		this.repository = repository;
		this.mapper = mapper;
		this.audit = audit;
	}

	public Page<MunicipioSummary> findAll(Boolean active, Pageable pageable) {
		Page<Municipio> page = (active == null)
				? repository.findAll(pageable)
				: repository.findByActive(active, pageable);
		return page.map(mapper::toSummary);
	}

	@Transactional
	public MunicipioSummary create(MunicipioCreateRequest request) {
		String normalized = NameNormalizer.normalize(request.nombre());
		if (repository.existsByNombreNormalizado(normalized)) {
			throw new ConflictException("Ya existe un municipio con el nombre: " + request.nombre());
		}

		Municipio municipio = new Municipio();
		municipio.setNombre(request.nombre().trim());
		municipio.setNombreNormalizado(normalized);
		municipio.setActive(true);

		Municipio saved = repository.save(municipio);
		audit.record("CREATE_MUNICIPIO", saved.getId());
		return mapper.toSummary(saved);
	}
}
