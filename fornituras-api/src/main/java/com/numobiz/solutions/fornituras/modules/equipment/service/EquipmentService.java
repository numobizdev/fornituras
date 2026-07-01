package com.numobiz.solutions.fornituras.modules.equipment.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.equipment.dto.BatchCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentDetail;
import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentSummary;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;
import com.numobiz.solutions.fornituras.modules.equipment.mapper.EquipmentMapper;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.catalog.CatalogCodes;
import com.numobiz.solutions.fornituras.modules.catalog.entity.CatalogItem;
import com.numobiz.solutions.fornituras.modules.catalog.repository.CatalogItemRepository;
import com.numobiz.solutions.fornituras.modules.catalog.service.CatalogService;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Lógica del inventario de fornituras: alta individual y por lote (atómica), consulta paginada con
 * filtros, ficha, búsqueda por código (server-side), edición no identitaria y cambio de estado con
 * reglas de ciclo de vida. La unicidad se compara sobre el código normalizado; la vigencia se
 * deriva al leer (no se persiste). Toda mutación queda auditada.
 */
@Service
@Transactional(readOnly = true)
public class EquipmentService {

	private final EquipmentRepository repository;
	private final EquipmentMapper mapper;
	private final CatalogItemRepository catalogItemRepository;
	private final CatalogService catalogService;
	private final WarehouseRepository warehouseRepository;
	private final EquipmentLifecycleQuery lifecycle;
	private final AuditWriter audit;

	public EquipmentService(
			EquipmentRepository repository,
			EquipmentMapper mapper,
			CatalogItemRepository catalogItemRepository,
			CatalogService catalogService,
			WarehouseRepository warehouseRepository,
			EquipmentLifecycleQuery lifecycle,
			AuditWriter audit) {
		this.repository = repository;
		this.mapper = mapper;
		this.catalogItemRepository = catalogItemRepository;
		this.catalogService = catalogService;
		this.warehouseRepository = warehouseRepository;
		this.lifecycle = lifecycle;
		this.audit = audit;
	}

	public Page<EquipmentSummary> findAll(
			String q, EquipmentStatus status, Long equipmentTypeId, Long sizeId, Long warehouseId, Pageable pageable) {
		Page<Equipment> page = repository.findAll(filterBy(q, status, equipmentTypeId, sizeId, warehouseId), pageable);
		List<Equipment> content = page.getContent();

		Map<Long, String> typeNames = resolve(
				ids(content, Equipment::getEquipmentTypeId),
				catalogItemRepository::findAllById, CatalogItem::getId, CatalogItem::getNombre);
		Map<Long, String> sizeLabels = resolve(
				ids(content, Equipment::getSizeId),
				catalogItemRepository::findAllById, CatalogItem::getId, CatalogItem::getNombre);
		Map<Long, String> warehouseNames = resolve(
				ids(content, Equipment::getWarehouseId),
				warehouseRepository::findAllById, Warehouse::getId, Warehouse::getNombre);

		LocalDate today = LocalDate.now();
		return page.map(e -> mapper.toSummary(
				e,
				typeNames.get(e.getEquipmentTypeId()),
				e.getSizeId() == null ? null : sizeLabels.get(e.getSizeId()),
				warehouseNames.get(e.getWarehouseId()),
				ExpiryCalculator.statusFor(e.getFechaVencimiento(), today)));
	}

	public EquipmentDetail findById(Long id) {
		return toDetail(getOrThrow(id));
	}

	/** Resolución server-side por código (consumible por 004/007/009); no filtra detalles si no existe. */
	public EquipmentDetail findByCodigo(String codigo) {
		String normalized = CodeNormalizer.normalize(codigo);
		Equipment equipment = repository.findByCodigoNormalizado(normalized)
				.orElseThrow(() -> new NotFoundException("Fornitura no encontrada para el código indicado."));
		return toDetail(equipment);
	}

	@Transactional
	public EquipmentDetail create(EquipmentCreateRequest request) {
		String normalized = requireValidCode(request.codigoQr());
		if (repository.existsByCodigoNormalizado(normalized)) {
			throw new ConflictException("Ya existe una fornitura con el código: " + request.codigoQr());
		}
		validateCatalogs(request.equipmentTypeId(), request.sizeId(), request.warehouseId());

		Equipment equipment = new Equipment();
		equipment.setCodigoQr(displayCode(request.codigoQr()));
		equipment.setCodigoNormalizado(normalized);
		equipment.setStatus(EquipmentStatus.DISPONIBLE);
		applyGeneral(equipment, request.equipmentTypeId(), request.sizeId(), request.warehouseId(),
				request.descripcion(), request.marca(), request.modelo(), request.nivelBalistico(),
				request.fechaFabricacion(), request.fechaAdquisicion(), request.vidaUtilMeses(),
				request.fechaVencimiento(), request.observaciones());
		equipment.setNumeroInventario(request.numeroInventario());
		equipment.setFotoUrl(request.fotoUrl());

		Equipment saved = repository.save(equipment);
		audit.record("CREATE_EQUIPMENT", saved.getId());
		return toDetail(saved);
	}

	/**
	 * Alta por lote: una sola transacción. Si cualquier código está duplicado (dentro del lote o
	 * contra la base) se aborta todo (atomicidad, US2).
	 */
	@Transactional
	public List<EquipmentDetail> createBatch(BatchCreateRequest request) {
		validateCatalogs(request.equipmentTypeId(), request.sizeId(), request.warehouseId());

		Set<String> seen = new HashSet<>();
		List<Equipment> toSave = new ArrayList<>();
		for (String raw : request.codigos()) {
			String normalized = requireValidCode(raw);
			if (!seen.add(normalized)) {
				throw new ConflictException("Código duplicado dentro del lote: " + raw);
			}
			if (repository.existsByCodigoNormalizado(normalized)) {
				throw new ConflictException("Ya existe una fornitura con el código: " + raw);
			}
			Equipment equipment = new Equipment();
			equipment.setCodigoQr(displayCode(raw));
			equipment.setCodigoNormalizado(normalized);
			equipment.setStatus(EquipmentStatus.DISPONIBLE);
			applyGeneral(equipment, request.equipmentTypeId(), request.sizeId(), request.warehouseId(),
					request.descripcion(), request.marca(), request.modelo(), request.nivelBalistico(),
					request.fechaFabricacion(), request.fechaAdquisicion(), request.vidaUtilMeses(),
					request.fechaVencimiento(), request.observaciones());
			toSave.add(equipment);
		}

		List<Equipment> saved = repository.saveAll(toSave);
		saved.forEach(s -> audit.record("CREATE_EQUIPMENT", s.getId()));
		return saved.stream().map(this::toDetail).toList();
	}

	@Transactional
	public EquipmentDetail update(Long id, EquipmentCreateRequest request) {
		Equipment equipment = getOrThrow(id);
		String normalized = requireValidCode(request.codigoQr());
		if (!normalized.equals(equipment.getCodigoNormalizado())) {
			// El código es ancla de identidad (grabado en la pieza): inmutable en la edición normal.
			throw new ConflictException("El código de la fornitura es inmutable; no puede cambiarse al editar.");
		}
		validateCatalogs(request.equipmentTypeId(), request.sizeId(), request.warehouseId());

		applyGeneral(equipment, request.equipmentTypeId(), request.sizeId(), request.warehouseId(),
				request.descripcion(), request.marca(), request.modelo(), request.nivelBalistico(),
				request.fechaFabricacion(), request.fechaAdquisicion(), request.vidaUtilMeses(),
				request.fechaVencimiento(), request.observaciones());
		equipment.setNumeroInventario(request.numeroInventario());
		equipment.setFotoUrl(request.fotoUrl());

		Equipment saved = repository.save(equipment);
		audit.record("UPDATE_EQUIPMENT", id);
		return toDetail(saved);
	}

	@Transactional
	public EquipmentDetail changeStatus(Long id, EquipmentStatus newStatus) {
		Equipment equipment = getOrThrow(id);
		if (newStatus == EquipmentStatus.BAJA_DEFINITIVA && lifecycle.hasActiveAssignment(id)) {
			throw new ConflictException("La fornitura tiene una asignación vigente; resuélvala antes de darla de baja.");
		}
		if (newStatus == EquipmentStatus.EN_TRASLADO && lifecycle.hasActiveAssignment(id)) {
			throw new ConflictException("La fornitura tiene una asignación vigente; resuélvala antes de trasladarla.");
		}
		equipment.setStatus(newStatus);
		Equipment saved = repository.save(equipment);
		audit.record("STATUS_CHANGE_EQUIPMENT", id);
		return toDetail(saved);
	}

	private void applyGeneral(
			Equipment equipment, Long equipmentTypeId, Long sizeId, Long warehouseId,
			String descripcion, String marca, String modelo, String nivelBalistico,
			LocalDate fechaFabricacion, LocalDate fechaAdquisicion, Integer vidaUtilMeses,
			LocalDate fechaVencimiento, String observaciones) {
		equipment.setEquipmentTypeId(equipmentTypeId);
		equipment.setSizeId(sizeId);
		equipment.setWarehouseId(warehouseId);
		equipment.setDescripcion(descripcion);
		equipment.setMarca(marca);
		equipment.setModelo(modelo);
		equipment.setNivelBalistico(nivelBalistico);
		equipment.setFechaFabricacion(fechaFabricacion);
		equipment.setFechaAdquisicion(fechaAdquisicion);
		equipment.setVidaUtilMeses(vidaUtilMeses);
		equipment.setFechaVencimiento(resolveExpiry(fechaVencimiento, fechaFabricacion, vidaUtilMeses));
		equipment.setObservaciones(observaciones);
	}

	/** Deriva la fecha de vencimiento si no se capturó pero hay fabricación + vida útil en meses. */
	private LocalDate resolveExpiry(LocalDate fechaVencimiento, LocalDate fechaFabricacion, Integer vidaUtilMeses) {
		if (fechaVencimiento != null) {
			return fechaVencimiento;
		}
		if (fechaFabricacion != null && vidaUtilMeses != null) {
			return fechaFabricacion.plusMonths(vidaUtilMeses);
		}
		return null;
	}

	private void validateCatalogs(Long equipmentTypeId, Long sizeId, Long warehouseId) {
		catalogService.requireActiveItem(equipmentTypeId, CatalogCodes.TIPO_FORNITURA);
		if (sizeId != null) {
			catalogService.requireActiveItem(sizeId, CatalogCodes.TALLA);
		}
		Warehouse warehouse = warehouseRepository.findById(warehouseId)
				.orElseThrow(() -> new BadRequestException("Almacén no encontrado: " + warehouseId));
		if (!warehouse.isActive()) {
			throw new BadRequestException("El almacén está inactivo.");
		}
	}

	private String requireValidCode(String raw) {
		String normalized = CodeNormalizer.normalize(raw);
		if (normalized.isBlank()) {
			throw new BadRequestException("El código (QR/serie) no es válido.");
		}
		return normalized;
	}

	private String displayCode(String raw) {
		return raw.trim().toUpperCase(Locale.ROOT);
	}

	private EquipmentDetail toDetail(Equipment equipment) {
		String tipoNombre = catalogService.resolveName(equipment.getEquipmentTypeId());
		String tallaEtiqueta = catalogService.resolveName(equipment.getSizeId());
		String almacenNombre = warehouseRepository.findById(equipment.getWarehouseId())
				.map(Warehouse::getNombre).orElse(null);
		ExpiryStatus vigencia = ExpiryCalculator.statusFor(equipment.getFechaVencimiento(), LocalDate.now());
		return mapper.toDetail(equipment, tipoNombre, tallaEtiqueta, almacenNombre, vigencia);
	}

	private Equipment getOrThrow(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Fornitura no encontrada: " + id));
	}

	private Specification<Equipment> filterBy(
			String q, EquipmentStatus status, Long equipmentTypeId, Long sizeId, Long warehouseId) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();
			if (status != null) {
				predicates.add(cb.equal(root.get("status"), status));
			}
			if (equipmentTypeId != null) {
				predicates.add(cb.equal(root.get("equipmentTypeId"), equipmentTypeId));
			}
			if (sizeId != null) {
				predicates.add(cb.equal(root.get("sizeId"), sizeId));
			}
			if (warehouseId != null) {
				predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
			}
			if (q != null && !q.isBlank()) {
				String byCode = "%" + CodeNormalizer.normalize(q) + "%";
				String byText = "%" + q.trim().toUpperCase(Locale.ROOT) + "%";
				predicates.add(cb.or(
						cb.like(root.get("codigoNormalizado"), byCode),
						cb.like(cb.upper(root.get("descripcion")), byText)));
			}
			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private <T> Set<Long> ids(Collection<Equipment> content, Function<Equipment, Long> extractor) {
		Set<Long> result = new HashSet<>();
		for (Equipment e : content) {
			Long id = extractor.apply(e);
			if (id != null) {
				result.add(id);
			}
		}
		return result;
	}

	private <T> Map<Long, String> resolve(
			Set<Long> ids,
			Function<Iterable<Long>, List<T>> finder,
			Function<T, Long> idExtractor,
			Function<T, String> nameExtractor) {
		if (ids.isEmpty()) {
			return Map.of();
		}
		return finder.apply(ids).stream().collect(Collectors.toMap(idExtractor, nameExtractor));
	}
}
