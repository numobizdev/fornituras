package com.numobiz.solutions.fornituras.modules.officers.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.crypto.BlindIndexer;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.catalog.CatalogCodes;
import com.numobiz.solutions.fornituras.modules.catalog.service.CatalogService;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerCreateRequest;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerDetail;
import com.numobiz.solutions.fornituras.modules.officers.dto.OfficerSummary;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.mapper.OfficerMapper;
import com.numobiz.solutions.fornituras.modules.officers.repository.OfficerRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Lógica del padrón de elementos. Centraliza lo sensible: búsqueda sobre datos cifrados (placa en
 * claro + blind index para CURP/RFC), <b>enmascaramiento de PII por rol</b> (el cliente nunca
 * decide la visibilidad) y <b>auditoría</b> del acceso a la ficha completa y de las altas. La PII
 * en claro (nombre/apellidos/CURP/RFC) se cifra en reposo vía el converter de la entidad (ADR 0006).
 *
 * <p>Sexo y tipo de sangre se resuelven contra el catálogo genérico ({@code SEXO}/{@code TIPO_SANGRE},
 * ADR 0007) mediante {@link CatalogService}, no contra tablas propias.
 */
@Service
@Transactional(readOnly = true)
public class OfficerService {

	private static final String ROLE_PII = "ROLE_ADMIN";

	private final OfficerRepository repository;
	private final OfficerMapper mapper;
	private final BlindIndexer blindIndexer;
	private final CatalogService catalogService;
	private final AuditWriter audit;

	public OfficerService(
			OfficerRepository repository,
			OfficerMapper mapper,
			BlindIndexer blindIndexer,
			CatalogService catalogService,
			AuditWriter audit) {
		this.repository = repository;
		this.mapper = mapper;
		this.blindIndexer = blindIndexer;
		this.catalogService = catalogService;
		this.audit = audit;
	}

	public Page<OfficerSummary> findAll(String q, String municipio, Long sexoId, Pageable pageable) {
		Page<Officer> page = repository.findAll(filterBy(q, municipio, sexoId), pageable);
		List<Officer> content = page.getContent();

		Set<Long> catalogIds = new HashSet<>();
		catalogIds.addAll(ids(content, Officer::getSexoId));
		catalogIds.addAll(ids(content, Officer::getTipoSangreId));
		Map<Long, String> names = catalogService.resolveNames(catalogIds);

		return page.map(o -> mapper.toSummary(
				o,
				names.get(o.getSexoId()),
				o.getTipoSangreId() == null ? null : names.get(o.getTipoSangreId())));
	}

	/** Devuelve la ficha (enmascarada por rol) y <b>audita</b> el acceso (FR-006). */
	public OfficerDetail findById(Long id) {
		Officer officer = getOrThrow(id);
		audit.record("VIEW_OFFICER", id);
		return toDetail(officer);
	}

	@Transactional
	public OfficerDetail create(OfficerCreateRequest request) {
		String placaNormalizada = CodeNormalizer.normalize(request.placa());
		if (placaNormalizada.isBlank()) {
			throw new BadRequestException("La placa no es válida.");
		}
		if (repository.existsByPlacaNormalizada(placaNormalizada)) {
			throw new ConflictException("Ya existe un elemento con la placa: " + request.placa());
		}
		String curpIdx = blindIndexer.index(request.curp());
		if (curpIdx != null && repository.existsByCurpIdx(curpIdx)) {
			throw new ConflictException("Ya existe un elemento con esa CURP.");
		}
		String rfcIdx = blindIndexer.index(request.rfc());
		if (rfcIdx != null && repository.existsByRfcIdx(rfcIdx)) {
			throw new ConflictException("Ya existe un elemento con ese RFC.");
		}
		validateCatalogs(request.sexoId(), request.tipoSangreId());

		Officer officer = new Officer();
		officer.setNombre(request.nombre().trim());
		officer.setApellidoPaterno(request.apellidoPaterno().trim());
		officer.setApellidoMaterno(blankToNull(request.apellidoMaterno()));
		officer.setPlaca(request.placa().trim().toUpperCase(Locale.ROOT));
		officer.setPlacaNormalizada(placaNormalizada);
		officer.setCurp(upperOrNull(request.curp()));
		officer.setCurpIdx(curpIdx);
		officer.setRfc(upperOrNull(request.rfc()));
		officer.setRfcIdx(rfcIdx);
		officer.setSexoId(request.sexoId());
		officer.setTipoSangreId(request.tipoSangreId());
		officer.setMunicipio(blankToNull(request.municipio()));
		officer.setEstado(blankToNull(request.estado()));
		officer.setFotoUrl(blankToNull(request.fotoUrl()));
		officer.setActive(true);

		Officer saved = repository.save(officer);
		audit.record("CREATE_OFFICER", saved.getId());
		return toDetail(saved);
	}

	/** El sexo y el tipo de sangre deben ser valores activos de sus catálogos (SEXO/TIPO_SANGRE). */
	private void validateCatalogs(Long sexoId, Long tipoSangreId) {
		catalogService.requireActiveItem(sexoId, CatalogCodes.SEXO);
		if (tipoSangreId != null) {
			catalogService.requireActiveItem(tipoSangreId, CatalogCodes.TIPO_SANGRE);
		}
	}

	private OfficerDetail toDetail(Officer officer) {
		String sexoNombre = catalogService.resolveName(officer.getSexoId());
		String tipoSangreEtiqueta = catalogService.resolveName(officer.getTipoSangreId());
		return mapper.toDetail(officer, sexoNombre, tipoSangreEtiqueta, canViewPii());
	}

	/** El servidor decide la visibilidad de la PII a partir del rol (solo ADMIN ve CURP/RFC). */
	private boolean canViewPii() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return auth != null && auth.getAuthorities().stream()
				.anyMatch(authority -> ROLE_PII.equals(authority.getAuthority()));
	}

	private Officer getOrThrow(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Elemento no encontrado: " + id));
	}

	private Specification<Officer> filterBy(String q, String municipio, Long sexoId) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (municipio != null && !municipio.isBlank()) {
				predicates.add(cb.like(cb.upper(root.get("municipio")),
						"%" + municipio.trim().toUpperCase(Locale.ROOT) + "%"));
			}
			if (sexoId != null) {
				predicates.add(cb.equal(root.get("sexoId"), sexoId));
			}
			if (q != null && !q.isBlank()) {
				List<Predicate> matches = new ArrayList<>();
				matches.add(cb.like(root.get("placaNormalizada"), "%" + CodeNormalizer.normalize(q) + "%"));
				String idx = blindIndexer.index(q);
				if (idx != null) {
					matches.add(cb.equal(root.get("curpIdx"), idx));
					matches.add(cb.equal(root.get("rfcIdx"), idx));
				}
				predicates.add(cb.or(matches.toArray(new Predicate[0])));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}

	private String upperOrNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim().toUpperCase(Locale.ROOT);
	}

	private Set<Long> ids(Collection<Officer> content, Function<Officer, Long> extractor) {
		Set<Long> result = new HashSet<>();
		for (Officer o : content) {
			Long id = extractor.apply(o);
			if (id != null) {
				result.add(id);
			}
		}
		return result;
	}
}
