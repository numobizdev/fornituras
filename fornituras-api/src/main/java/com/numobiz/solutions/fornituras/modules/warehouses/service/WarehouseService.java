package com.numobiz.solutions.fornituras.modules.warehouses.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.common.text.NameNormalizer;
import com.numobiz.solutions.fornituras.modules.municipios.repository.MunicipioRepository;
import com.numobiz.solutions.fornituras.modules.users.repository.UserRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseCreateRequest;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseDetail;
import com.numobiz.solutions.fornituras.modules.warehouses.dto.WarehouseSummary;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.WarehouseType;
import com.numobiz.solutions.fornituras.modules.warehouses.mapper.WarehouseMapper;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WarehouseService {

	private final WarehouseRepository repository;
	private final WarehouseMapper mapper;
	private final WarehouseUsageQuery usageQuery;
	private final MunicipioRepository municipioRepository;
	private final UserRepository userRepository;
	private final AuditWriter audit;

	public WarehouseService(
			WarehouseRepository repository,
			WarehouseMapper mapper,
			WarehouseUsageQuery usageQuery,
			MunicipioRepository municipioRepository,
			UserRepository userRepository,
			AuditWriter audit) {
		this.repository = repository;
		this.mapper = mapper;
		this.usageQuery = usageQuery;
		this.municipioRepository = municipioRepository;
		this.userRepository = userRepository;
		this.audit = audit;
	}

	public Page<WarehouseSummary> findAll(Boolean active, WarehouseType tipo, Pageable pageable) {
		Page<Warehouse> page;
		if (active != null && tipo != null) {
			page = repository.findByActiveAndTipo(active, tipo, pageable);
		} else if (active != null) {
			page = repository.findByActive(active, pageable);
		} else if (tipo != null) {
			page = repository.findByTipo(tipo, pageable);
		} else {
			page = repository.findAll(pageable);
		}
		return page.map(mapper::toSummary);
	}

	public WarehouseDetail findById(Long id) {
		Warehouse warehouse = getOrThrow(id);
		return toDetail(warehouse);
	}

	@Transactional
	public WarehouseDetail create(WarehouseCreateRequest request) {
		String normalized = NameNormalizer.normalize(request.nombre());
		String codigo = request.codigo().trim();

		if (repository.existsByCodigoIgnoreCase(codigo)) {
			throw new ConflictException("Ya existe un almacén con la clave: " + codigo);
		}
		if (repository.existsByNombreNormalizado(normalized)) {
			throw new ConflictException("Ya existe un almacén con el nombre: " + request.nombre());
		}
		validateReferences(request);

		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo(codigo);
		warehouse.setNombreNormalizado(normalized);
		apply(warehouse, request);
		warehouse.setActive(true);

		Warehouse saved = repository.save(warehouse);
		audit.record("CREATE_WAREHOUSE", saved.getId());
		return toDetail(saved);
	}

	@Transactional
	public WarehouseDetail update(Long id, WarehouseCreateRequest request) {
		Warehouse warehouse = getOrThrow(id);
		String normalized = NameNormalizer.normalize(request.nombre());
		String codigo = request.codigo().trim();

		repository.findByCodigoIgnoreCase(codigo)
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ConflictException("Ya existe un almacén con la clave: " + codigo);
				});
		repository.findByNombreNormalizado(normalized)
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ConflictException("Ya existe un almacén con el nombre: " + request.nombre());
				});
		validateReferences(request);

		warehouse.setCodigo(codigo);
		warehouse.setNombreNormalizado(normalized);
		apply(warehouse, request);

		Warehouse saved = repository.save(warehouse);
		audit.record("UPDATE_WAREHOUSE", saved.getId());
		return toDetail(saved);
	}

	/**
	 * Desactivación lógica (nunca borrado físico): un almacén en uso seguiría siendo referenciado,
	 * así que solo se marca inactivo y deja de ofrecerse como ubicación (001) o destino (007).
	 */
	@Transactional
	public void deactivate(Long id) {
		Warehouse warehouse = getOrThrow(id);
		warehouse.setActive(false);
		repository.save(warehouse);
		audit.record("DEACTIVATE_WAREHOUSE", id);
	}

	/**
	 * Borrado físico solo si el almacén NO está en uso. Si tiene fornituras o traslados asociados,
	 * se bloquea y se ofrece la desactivación en su lugar (integridad referencial, FR-003).
	 */
	@Transactional
	public void delete(Long id) {
		Warehouse warehouse = getOrThrow(id);
		if (usageQuery.countUsage(id) > 0) {
			throw new ConflictException(
					"El almacén tiene fornituras o traslados asociados; desactívelo en vez de eliminarlo.");
		}
		repository.delete(warehouse);
		audit.record("DELETE_WAREHOUSE", id);
	}

	private void apply(Warehouse warehouse, WarehouseCreateRequest request) {
		warehouse.setNombre(request.nombre().trim());
		warehouse.setTipo(request.tipo());
		warehouse.setMunicipioId(request.municipioId());
		warehouse.setDireccion(request.direccion());
		warehouse.setCp(request.cp());
		warehouse.setLatitud(request.latitud());
		warehouse.setLongitud(request.longitud());
		warehouse.setResponsableId(request.responsableId());
		warehouse.setTelefono(request.telefono());
		warehouse.setEmailContacto(request.emailContacto());
		warehouse.setCapacidad(request.capacidad());
		warehouse.setObservaciones(request.observaciones());
	}

	private void validateReferences(WarehouseCreateRequest request) {
		if (request.municipioId() != null && !municipioRepository.existsById(request.municipioId())) {
			throw new BadRequestException("Municipio no encontrado: " + request.municipioId());
		}
		if (request.responsableId() != null && !userRepository.existsById(request.responsableId())) {
			throw new BadRequestException("Usuario responsable no encontrado: " + request.responsableId());
		}
	}

	private WarehouseDetail toDetail(Warehouse warehouse) {
		long ocupacion = usageQuery.countUsage(warehouse.getId());
		Double porcentaje = (warehouse.getCapacidad() != null && warehouse.getCapacidad() > 0)
				? (ocupacion * 100.0 / warehouse.getCapacidad())
				: null;
		return mapper.toDetail(warehouse, ocupacion, porcentaje);
	}

	private Warehouse getOrThrow(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Almacén no encontrado: " + id));
	}
}
