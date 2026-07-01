package com.numobiz.solutions.fornituras.modules.decommissions;

import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.decommissions.entity.Decommission;
import com.numobiz.solutions.fornituras.modules.decommissions.entity.DecommissionReason;
import com.numobiz.solutions.fornituras.modules.decommissions.repository.DecommissionReasonRepository;
import com.numobiz.solutions.fornituras.modules.decommissions.repository.DecommissionRepository;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.transfers.entity.Transfer;
import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferItem;
import com.numobiz.solutions.fornituras.modules.transfers.entity.TransferStatus;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferItemRepository;
import com.numobiz.solutions.fornituras.modules.transfers.repository.TransferRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base común de las pruebas de contrato/integración de bajas (feature 009): arranca la aplicación
 * completa sobre el perfil H2 en memoria con MockMvc y seguridad real. Antes de cada prueba deja
 * limpias las tablas implicadas y siembra un almacén, una fornitura disponible y un motivo de baja
 * activo (los motivos base solo se siembran por Flyway, que está deshabilitado en tests).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class DecommissionApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected DecommissionRepository decommissionRepository;
	@Autowired
	protected DecommissionReasonRepository reasonRepository;
	@Autowired
	protected EquipmentRepository equipmentRepository;
	@Autowired
	protected WarehouseRepository warehouseRepository;
	@Autowired
	protected AssignmentRepository assignmentRepository;
	@Autowired
	protected TransferRepository transferRepository;
	@Autowired
	protected TransferItemRepository transferItemRepository;

	protected Seed seed;
	private long warehouseId;

	protected record Seed(long warehouseId, long equipmentId, String equipmentCodigo, long motivoId) {
	}

	@BeforeEach
	void cleanAndSeed() {
		decommissionRepository.deleteAll();
		reasonRepository.deleteAll();
		transferItemRepository.deleteAll();
		transferRepository.deleteAll();
		assignmentRepository.deleteAll();
		equipmentRepository.deleteAll();
		warehouseRepository.deleteAll();

		warehouseId = seedWarehouse();
		long equipmentId = seedEquipment("FOR-B1", EquipmentStatus.DISPONIBLE);
		long motivoId = seedReason("Daño");
		seed = new Seed(warehouseId, equipmentId, "FOR-B1", motivoId);
	}

	private long seedWarehouse() {
		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo("ALM-BAJ");
		warehouse.setNombre("Almacén Bajas");
		warehouse.setNombreNormalizado("ALMACEN BAJAS");
		warehouse.setTipoItemId(1L);
		warehouse.setActive(true);
		return warehouseRepository.save(warehouse).getId();
	}

	protected long seedEquipment(String codigo, EquipmentStatus status) {
		Equipment equipment = new Equipment();
		equipment.setCodigoQr(codigo.trim().toUpperCase());
		equipment.setCodigoNormalizado(CodeNormalizer.normalize(codigo));
		equipment.setEquipmentTypeId(1L);
		equipment.setWarehouseId(warehouseId);
		equipment.setStatus(status);
		equipment.setDescripcion("Chaleco de prueba " + codigo);
		return equipmentRepository.save(equipment).getId();
	}

	/** Crea un almacén activo adicional (p. ej. destino de un traslado) y devuelve su id. */
	protected long seedActiveWarehouse(String codigo, String nombre) {
		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo(codigo);
		warehouse.setNombre(nombre);
		warehouse.setNombreNormalizado(nombre.toUpperCase());
		warehouse.setTipoItemId(1L);
		warehouse.setActive(true);
		return warehouseRepository.save(warehouse).getId();
	}

	protected long seedEquipmentOfType(String codigo, EquipmentStatus status, long equipmentTypeId) {
		Equipment equipment = new Equipment();
		equipment.setCodigoQr(codigo.trim().toUpperCase());
		equipment.setCodigoNormalizado(CodeNormalizer.normalize(codigo));
		equipment.setEquipmentTypeId(equipmentTypeId);
		equipment.setWarehouseId(warehouseId);
		equipment.setStatus(status);
		equipment.setDescripcion("Chaleco de prueba " + codigo);
		return equipmentRepository.save(equipment).getId();
	}

	protected long seedReason(String nombre) {
		DecommissionReason reason = new DecommissionReason();
		reason.setNombre(nombre);
		reason.setActive(true);
		return reasonRepository.save(reason).getId();
	}

	protected long seedDecommission(long equipmentId, long motivoId, LocalDate fecha) {
		Decommission decommission = new Decommission();
		decommission.setEquipmentId(equipmentId);
		decommission.setMotivoId(motivoId);
		decommission.setFecha(fecha);
		return decommissionRepository.save(decommission).getId();
	}

	/** Deja la fornitura con una asignación vigente (fecha_devolucion NULL) para probar el bloqueo. */
	protected void seedActiveAssignment(long equipmentId) {
		Assignment assignment = new Assignment();
		assignment.setEquipmentId(equipmentId);
		assignment.setOfficerId(1L);
		assignment.setFechaAsignacion(LocalDateTime.now());
		assignmentRepository.save(assignment);
	}

	/** Deja la fornitura dentro de un traslado en curso (estado ENVIADO) para probar el bloqueo. */
	protected void seedOngoingTransfer(long equipmentId) {
		Transfer transfer = new Transfer();
		transfer.setOrigenId(warehouseId);
		transfer.setDestinoId(warehouseId);
		transfer.setStatus(TransferStatus.ENVIADO);
		transfer.setFechaEnvio(LocalDateTime.now());
		long transferId = transferRepository.save(transfer).getId();

		TransferItem item = new TransferItem();
		item.setTransferId(transferId);
		item.setEquipmentId(equipmentId);
		transferItemRepository.save(item);
	}

	protected String decommissionJson(String codigo, Long motivoId, String observaciones) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("codigo", codigo);
		body.put("motivoId", motivoId);
		body.put("observaciones", observaciones);
		return objectMapper.writeValueAsString(body);
	}
}
