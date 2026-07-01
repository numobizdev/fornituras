package com.numobiz.solutions.fornituras.modules.dashboard.service;

import com.numobiz.solutions.fornituras.modules.dashboard.dto.DashboardSummary;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository.StatusTally;
import com.numobiz.solutions.fornituras.modules.equipment.service.ExpiryCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula los indicadores del tablero (010) con <b>consultas agregadas</b> del lado servidor: nunca
 * trae el inventario al cliente ni expone PII. Los estados operativos salen de un único
 * {@code GROUP BY status}; la vigencia (próximas a vencer/caducadas) se deriva de
 * {@code fecha_vencimiento} con el mismo criterio de {@link ExpiryCalculator} que usan 001/008, de
 * modo que cada contador coincide con el listado filtrado equivalente (SC-002).
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

	private final EquipmentRepository equipmentRepository;

	public DashboardService(EquipmentRepository equipmentRepository) {
		this.equipmentRepository = equipmentRepository;
	}

	public DashboardSummary summary() {
		LocalDate today = LocalDate.now();
		Map<EquipmentStatus, Long> byStatus = tallyByStatus();

		long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
		long disponibles = byStatus.getOrDefault(EquipmentStatus.DISPONIBLE, 0L);
		long asignadas = byStatus.getOrDefault(EquipmentStatus.ASIGNADA, 0L);
		long enMantenimiento = byStatus.getOrDefault(EquipmentStatus.EN_MANTENIMIENTO, 0L);

		long caducadas = equipmentRepository
				.countByFechaVencimientoLessThanAndStatusNot(today, EquipmentStatus.BAJA_DEFINITIVA);
		LocalDate warningLimit = today.plusDays(ExpiryCalculator.WARNING_WINDOW_DAYS);
		long proximasAVencer = equipmentRepository
				.countByFechaVencimientoBetweenAndStatusNot(today, warningLimit, EquipmentStatus.BAJA_DEFINITIVA);

		return new DashboardSummary(total, disponibles, asignadas, proximasAVencer, caducadas, enMantenimiento);
	}

	private Map<EquipmentStatus, Long> tallyByStatus() {
		List<StatusTally> tallies = equipmentRepository.tallyByStatus();
		Map<EquipmentStatus, Long> byStatus = new EnumMap<>(EquipmentStatus.class);
		for (StatusTally tally : tallies) {
			byStatus.put(tally.getStatus(), tally.getTotal());
		}
		return byStatus;
	}
}
