package com.numobiz.solutions.fornituras.modules.equipmenttypes.service;

import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.SizeCreateRequest;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.dto.SizeSummary;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.EquipmentType;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.Size;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.mapper.SizeMapper;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.EquipmentTypeRepository;
import com.numobiz.solutions.fornituras.modules.equipmenttypes.repository.SizeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SizeService {

	private final SizeRepository repository;
	private final EquipmentTypeRepository equipmentTypeRepository;
	private final SizeMapper mapper;
	private final CatalogAuditWriter audit;

	public SizeService(
			SizeRepository repository,
			EquipmentTypeRepository equipmentTypeRepository,
			SizeMapper mapper,
			CatalogAuditWriter audit) {
		this.repository = repository;
		this.equipmentTypeRepository = equipmentTypeRepository;
		this.mapper = mapper;
		this.audit = audit;
	}

	public List<SizeSummary> findAll(Long equipmentTypeId) {
		List<Size> sizes = (equipmentTypeId == null)
				? repository.findByActiveTrue()
				: repository.findByEquipmentTypeIdAndActiveTrue(equipmentTypeId);
		return mapper.toSummaryList(sizes);
	}

	@Transactional
	public SizeSummary create(SizeCreateRequest request) {
		Size size = new Size();
		size.setEtiqueta(request.etiqueta().trim());
		size.setActive(true);
		if (request.equipmentTypeId() != null) {
			size.setEquipmentType(resolveType(request.equipmentTypeId()));
		}

		Size saved = repository.save(size);
		audit.record("CREATE_SIZE", saved.getId());
		return mapper.toSummary(saved);
	}

	@Transactional
	public void deactivate(Long id) {
		Size size = repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Talla no encontrada: " + id));
		size.setActive(false);
		repository.save(size);
		audit.record("DEACTIVATE_SIZE", id);
	}

	private EquipmentType resolveType(Long equipmentTypeId) {
		return equipmentTypeRepository.findById(equipmentTypeId)
				.orElseThrow(() -> new NotFoundException("Tipo de fornitura no encontrado: " + equipmentTypeId));
	}
}
