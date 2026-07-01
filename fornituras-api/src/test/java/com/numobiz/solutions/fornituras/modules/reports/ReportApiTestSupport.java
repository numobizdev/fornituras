package com.numobiz.solutions.fornituras.modules.reports;

import com.numobiz.solutions.fornituras.common.crypto.BlindIndexer;
import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.incidents.entity.Incident;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentStatus;
import com.numobiz.solutions.fornituras.modules.incidents.entity.IncidentType;
import com.numobiz.solutions.fornituras.modules.incidents.repository.IncidentRepository;
import com.numobiz.solutions.fornituras.modules.officers.entity.Officer;
import com.numobiz.solutions.fornituras.modules.officers.repository.OfficerRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Base de las pruebas de reportes (011): arranca la app completa sobre H2 con MockMvc y seguridad
 * real, limpia las tablas relacionadas y ofrece helpers para sembrar fornituras, elementos (con PII
 * cifrada real), asignaciones vigentes e incidencias.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class ReportApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected EquipmentRepository equipmentRepository;
	@Autowired
	protected WarehouseRepository warehouseRepository;
	@Autowired
	protected OfficerRepository officerRepository;
	@Autowired
	protected AssignmentRepository assignmentRepository;
	@Autowired
	protected IncidentRepository incidentRepository;
	@Autowired
	protected BlindIndexer blindIndexer;

	private long warehouseId;

	@BeforeEach
	void cleanAndSeed() {
		assignmentRepository.deleteAll();
		incidentRepository.deleteAll();
		equipmentRepository.deleteAll();
		officerRepository.deleteAll();
		warehouseRepository.deleteAll();
		warehouseId = seedWarehouse();
	}

	private long seedWarehouse() {
		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo("ALM-REP");
		warehouse.setNombre("Almacén Reportes");
		warehouse.setNombreNormalizado("ALMACEN REPORTES");
		warehouse.setTipoItemId(1L);
		warehouse.setActive(true);
		return warehouseRepository.save(warehouse).getId();
	}

	protected long seedEquipment(String codigo, EquipmentStatus status) {
		Equipment equipment = new Equipment();
		equipment.setCodigoQr(codigo.trim().toUpperCase(Locale.ROOT));
		equipment.setCodigoNormalizado(CodeNormalizer.normalize(codigo));
		equipment.setEquipmentTypeId(1L);
		equipment.setWarehouseId(warehouseId);
		equipment.setStatus(status);
		return equipmentRepository.save(equipment).getId();
	}

	protected long seedOfficer(
			String nombre, String apellido, String placa, String curp, String rfc, String municipio) {
		Officer officer = new Officer();
		officer.setNombre(nombre);
		officer.setApellidoPaterno(apellido);
		officer.setPlaca(placa.trim().toUpperCase(Locale.ROOT));
		officer.setPlacaNormalizada(CodeNormalizer.normalize(placa));
		officer.setCurp(curp);
		officer.setCurpIdx(blindIndexer.index(curp));
		officer.setRfc(rfc);
		officer.setRfcIdx(blindIndexer.index(rfc));
		officer.setSexoId(1L);
		officer.setMunicipio(municipio);
		officer.setEstado("Jalisco");
		officer.setActive(true);
		return officerRepository.save(officer).getId();
	}

	protected long seedActiveAssignment(long equipmentId, long officerId) {
		Assignment assignment = new Assignment();
		assignment.setEquipmentId(equipmentId);
		assignment.setOfficerId(officerId);
		assignment.setFechaAsignacion(LocalDateTime.now());
		return assignmentRepository.save(assignment).getId();
	}

	protected long seedIncident(long equipmentId, IncidentStatus estado) {
		Incident incident = new Incident();
		incident.setEquipmentId(equipmentId);
		incident.setTipo(IncidentType.DANO);
		incident.setDescripcion("Incidencia de prueba");
		incident.setEstado(estado);
		incident.setFechaReporte(LocalDateTime.now());
		return incidentRepository.save(incident).getId();
	}
}
