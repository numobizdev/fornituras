package com.numobiz.solutions.fornituras.modules.incidents;

import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.incidents.entity.Incident;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentType;
import com.numobiz.solutions.fornituras.modules.incidents.repository.IncidentRepository;
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
 * Base común de las pruebas de contrato/integración de incidencias (feature 008): arranca la
 * aplicación completa sobre el perfil H2 en memoria con MockMvc y seguridad real. Antes de cada prueba
 * deja limpias las tablas de incidencia/fornitura/almacén y siembra un almacén y una fornitura
 * disponible, exponiendo sus id vía {@link Seed}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class IncidentApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected IncidentRepository incidentRepository;
	@Autowired
	protected EquipmentRepository equipmentRepository;
	@Autowired
	protected WarehouseRepository warehouseRepository;

	protected Seed seed;
	private long warehouseId;

	protected record Seed(long warehouseId, long equipmentId) {
	}

	@BeforeEach
	void cleanAndSeed() {
		incidentRepository.deleteAll();
		equipmentRepository.deleteAll();
		warehouseRepository.deleteAll();

		warehouseId = seedWarehouse();
		long equipmentId = seedEquipment("FOR-I1", EquipmentStatus.DISPONIBLE, null);
		seed = new Seed(warehouseId, equipmentId);
	}

	private long seedWarehouse() {
		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo("ALM-INC");
		warehouse.setNombre("Almacén Incidencias");
		warehouse.setNombreNormalizado("ALMACEN INCIDENCIAS");
		warehouse.setTipoItemId(1L);
		warehouse.setActive(true);
		return warehouseRepository.save(warehouse).getId();
	}

	protected long seedEquipment(String codigo, EquipmentStatus status, LocalDate fechaVencimiento) {
		Equipment equipment = new Equipment();
		equipment.setCodigoQr(codigo.trim().toUpperCase());
		equipment.setCodigoNormalizado(CodeNormalizer.normalize(codigo));
		equipment.setEquipmentTypeId(1L);
		equipment.setWarehouseId(warehouseId);
		equipment.setStatus(status);
		equipment.setFechaVencimiento(fechaVencimiento);
		return equipmentRepository.save(equipment).getId();
	}

	protected long seedIncident(long equipmentId, IncidentType tipo, IncidentStatus estado) {
		Incident incident = new Incident();
		incident.setEquipmentId(equipmentId);
		incident.setTipo(tipo);
		incident.setDescripcion("Incidencia de prueba");
		incident.setEstado(estado);
		incident.setFechaReporte(LocalDateTime.now());
		return incidentRepository.save(incident).getId();
	}

	protected String reportJson(long equipmentId, String tipo, String descripcion) throws Exception {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("equipmentId", equipmentId);
		body.put("tipo", tipo);
		body.put("descripcion", descripcion);
		return objectMapper.writeValueAsString(body);
	}
}
