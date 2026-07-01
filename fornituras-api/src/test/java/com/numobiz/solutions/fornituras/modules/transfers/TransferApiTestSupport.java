package com.numobiz.solutions.fornituras.modules.transfers;

import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.transfers.entity.Transfer;
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

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base común de las pruebas de contrato/integración de traslados (feature 007): arranca la aplicación
 * completa sobre el perfil H2 en memoria ({@code application-test.yml}) con MockMvc y seguridad real.
 * Antes de cada prueba deja limpias las tablas de traslado/fornitura/almacén y siembra dos almacenes
 * (origen y destino, activos) y dos fornituras disponibles en el origen, exponiendo sus id vía
 * {@link Seed}. Estas pruebas saldan la deuda diferida (T009/T010/T017/T021): la infra H2/MockMvc ya
 * existe, así que el contrato se verifica extremo a extremo, no solo a nivel unitario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class TransferApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected TransferRepository transferRepository;
	@Autowired
	protected TransferItemRepository transferItemRepository;
	@Autowired
	protected EquipmentRepository equipmentRepository;
	@Autowired
	protected WarehouseRepository warehouseRepository;

	protected Seed seed;

	/** Referencias sembradas y reutilizables por cada prueba. */
	protected record Seed(long origenId, long destinoId, long equipment1Id, long equipment2Id) {
	}

	@BeforeEach
	void cleanAndSeed() {
		transferItemRepository.deleteAll();
		transferRepository.deleteAll();
		equipmentRepository.deleteAll();
		warehouseRepository.deleteAll();

		long origenId = seedWarehouse("ALM-ORI", "Almacén Origen", true);
		long destinoId = seedWarehouse("ALM-DES", "Almacén Destino", true);
		long e1 = seedEquipment("FOR-T1", origenId, EquipmentStatus.DISPONIBLE);
		long e2 = seedEquipment("FOR-T2", origenId, EquipmentStatus.DISPONIBLE);
		seed = new Seed(origenId, destinoId, e1, e2);
	}

	protected long seedWarehouse(String codigo, String nombre, boolean active) {
		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo(codigo);
		warehouse.setNombre(nombre);
		warehouse.setNombreNormalizado(nombre.toUpperCase());
		warehouse.setTipoItemId(1L);
		warehouse.setActive(active);
		return warehouseRepository.save(warehouse).getId();
	}

	protected long seedEquipment(String codigo, long warehouseId, EquipmentStatus status) {
		Equipment equipment = new Equipment();
		equipment.setCodigoQr(codigo.trim().toUpperCase());
		equipment.setCodigoNormalizado(CodeNormalizer.normalize(codigo));
		equipment.setEquipmentTypeId(1L);
		equipment.setWarehouseId(warehouseId);
		equipment.setStatus(status);
		return equipmentRepository.save(equipment).getId();
	}

	/** Persiste un traslado directamente (sin pasar por la API), con estado dado. */
	protected long seedTransfer(long origenId, long destinoId, TransferStatus status) {
		Transfer transfer = new Transfer();
		transfer.setOrigenId(origenId);
		transfer.setDestinoId(destinoId);
		transfer.setStatus(status);
		transfer.setFechaEnvio(LocalDateTime.now());
		return transferRepository.save(transfer).getId();
	}

	/** JSON de alta de traslado. */
	protected String createJson(long origenId, long destinoId, List<Long> equipmentIds) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("origenId", origenId);
		body.put("destinoId", destinoId);
		body.put("equipmentIds", equipmentIds);
		return objectMapper.writeValueAsString(body);
	}
}
