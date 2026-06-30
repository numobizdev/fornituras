package com.numobiz.solutions.fornituras.modules.equipmenttypes.service;

import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeDetail;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.EquipmentTypeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.SizeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.EquipmentType;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.EquipmentTypeMapper;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.SizeMapper;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.EquipmentTypeRepository;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.SizeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EquipmentTypeService {

	private static final Logger log = LoggerFactory.getLogger(EquipmentTypeService.class);

	private final EquipmentTypeRepository repository;
	private final SizeRepository sizeRepository;
	private final EquipmentTypeMapper mapper;
	private final SizeMapper sizeMapper;
	private final CatalogAuditWriter audit;

	public EquipmentTypeService(
			EquipmentTypeRepository repository,
			SizeRepository sizeRepository,
			EquipmentTypeMapper mapper,
			SizeMapper sizeMapper,
			CatalogAuditWriter audit) {
		this.repository = repository;
		this.sizeRepository = sizeRepository;
		this.mapper = mapper;
		this.sizeMapper = sizeMapper;
		this.audit = audit;
	}

	public Page<EquipmentTypeSummary> findAll(Boolean active, Pageable pageable) {
		Page<EquipmentType> page = (active == null)
				? repository.findAll(pageable)
				: repository.findByActive(active, pageable);
		return page.map(mapper::toSummary);
	}

	public EquipmentTypeDetail findById(Long id) {
		EquipmentType type = getOrThrow(id);
		List<SizeSummary> sizes = sizeMapper.toSummaryList(
				sizeRepository.findByEquipmentTypeIdAndActiveTrue(id));
		return mapper.toDetail(type, sizes);
	}

	@Transactional
	public EquipmentTypeDetail create(EquipmentTypeCreateRequest request) {
		String normalized = NameNormalizer.normalize(request.nombre());
		if (repository.existsByNombreNormalizado(normalized)) {
			throw new ConflictException("Ya existe un tipo de fornitura con el nombre: " + request.nombre());
		}

		EquipmentType type = new EquipmentType();
		type.setNombre(request.nombre().trim());
		type.setNombreNormalizado(normalized);
		type.setDescripcion(request.descripcion());
		type.setFotoUrl(request.fotoUrl());
		type.setActive(true);

		EquipmentType saved = repository.save(type);
		audit.record("CREATE_EQUIPMENT_TYPE", saved.getId());
		return mapper.toDetail(saved, List.of());
	}

	@Transactional
	public EquipmentTypeDetail update(Long id, EquipmentTypeCreateRequest request) {
		EquipmentType type = getOrThrow(id);
		String normalized = NameNormalizer.normalize(request.nombre());

		repository.findByNombreNormalizado(normalized)
				.filter(existing -> !existing.getId().equals(id))
				.ifPresent(existing -> {
					throw new ConflictException("Ya existe un tipo de fornitura con el nombre: " + request.nombre());
				});

		type.setNombre(request.nombre().trim());
		type.setNombreNormalizado(normalized);
		type.setDescripcion(request.descripcion());
		type.setFotoUrl(request.fotoUrl());

		EquipmentType saved = repository.save(type);
		audit.record("UPDATE_EQUIPMENT_TYPE", saved.getId());
		List<SizeSummary> sizes = sizeMapper.toSummaryList(
				sizeRepository.findByEquipmentTypeIdAndActiveTrue(id));
		return mapper.toDetail(saved, sizes);
	}

	/**
	 * Desactivación lógica (nunca borrado físico): un tipo en uso por fornituras seguiría
	 * siendo referenciado, así que solo se marca inactivo y deja de ofrecerse en el alta de
	 * 001. La integridad referencial se preserva por diseño.
	 */
	@Transactional
	public void deactivate(Long id) {
		EquipmentType type = getOrThrow(id);
		type.setActive(false);
		repository.save(type);
		audit.record("DEACTIVATE_EQUIPMENT_TYPE", id);
	}

	private EquipmentType getOrThrow(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Tipo de fornitura no encontrado: " + id));
	}
}
