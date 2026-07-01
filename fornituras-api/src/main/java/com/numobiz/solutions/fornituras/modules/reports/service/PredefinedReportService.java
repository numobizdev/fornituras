package com.numobiz.solutions.fornituras.modules.reports.service;

import com.numobiz.solutions.fornituras.modules.equipment.dto.EquipmentSummary;
import com.numobiz.solutions.fornituras.modules.equipment.service.EquipmentService;
import com.numobiz.solutions.fornituras.modules.reports.dto.PredefinedReportType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reportes operativos predefinidos (US3): reutiliza el listado de fornituras de 001 filtrando por el
 * estado que define cada {@link PredefinedReportType}. No reimplementa consultas: delega en
 * {@link EquipmentService} para no duplicar reglas de inventario. Sin PII (las fornituras no la tienen).
 */
@Service
@Transactional(readOnly = true)
public class PredefinedReportService {

	private final EquipmentService equipmentService;

	public PredefinedReportService(EquipmentService equipmentService) {
		this.equipmentService = equipmentService;
	}

	public Page<EquipmentSummary> report(PredefinedReportType tipo, Pageable pageable) {
		return equipmentService.findAll(null, tipo.status(), null, null, null, pageable);
	}

	/** Lista completa (sin paginar) para exportación. */
	public List<EquipmentSummary> reportRows(PredefinedReportType tipo) {
		return equipmentService.findAll(null, tipo.status(), null, null, null, Pageable.unpaged()).getContent();
	}
}
