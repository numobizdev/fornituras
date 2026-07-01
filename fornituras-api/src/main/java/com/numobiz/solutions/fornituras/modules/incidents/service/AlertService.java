package com.numobiz.solutions.fornituras.modules.incidents.service;

import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.entity.ExpiryStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.service.ExpiryCalculator;
import com.numobiz.solutions.fornituras.modules.incidents.dto.AlertItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Deriva las alertas de vigencia a partir de {@code equipment.fecha_vencimiento}, sin materializarlas.
 * Reutiliza {@link ExpiryCalculator} (misma ventana de ≤ 90 días que 001/010: única fuente de verdad
 * del umbral) para clasificar cada fornitura como próxima a vencer (preventiva) o caducada (crítica).
 * La consulta acota a las candidatas dentro de la ventana; las bajas definitivas se excluyen.
 */
@Service
@Transactional(readOnly = true)
public class AlertService {

	private final EquipmentRepository equipmentRepository;

	public AlertService(EquipmentRepository equipmentRepository) {
		this.equipmentRepository = equipmentRepository;
	}

	/** Alertas de vigencia ordenadas con las críticas (caducadas) primero, luego por fecha ascendente. */
	public List<AlertItem> vigenciaAlerts() {
		LocalDate today = LocalDate.now();
		LocalDate threshold = today.plusDays(ExpiryCalculator.WARNING_WINDOW_DAYS);

		return equipmentRepository
				.findByFechaVencimientoLessThanEqualAndStatusNot(threshold, EquipmentStatus.BAJA_DEFINITIVA)
				.stream()
				.map(equipment -> toAlert(equipment, today))
				.filter(alert -> alert.expiryStatus() != ExpiryStatus.VIGENTE)
				.sorted(Comparator
						.comparing(AlertItem::expiryStatus, criticalFirst())
						.thenComparing(AlertItem::fechaVencimiento))
				.toList();
	}

	private AlertItem toAlert(Equipment equipment, LocalDate today) {
		ExpiryStatus status = ExpiryCalculator.statusFor(equipment.getFechaVencimiento(), today);
		return new AlertItem(
				equipment.getId(),
				equipment.getCodigoQr(),
				equipment.getDescripcion(),
				equipment.getFechaVencimiento(),
				status);
	}

	/** Caducada antes que próxima a vencer (crítica primero). */
	private Comparator<ExpiryStatus> criticalFirst() {
		return Comparator.comparingInt(status -> status == ExpiryStatus.CADUCADA ? 0 : 1);
	}
}
