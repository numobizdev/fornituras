package com.numobiz.solutions.fornituras.modules.dashboard;

import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

/**
 * Base de las pruebas del tablero (feature 010): arranca la aplicación completa sobre el perfil H2 en
 * memoria con MockMvc y seguridad real, deja limpias las tablas de fornitura/almacén y siembra un
 * almacén. Cada prueba siembra las fornituras que necesita vía {@link #seedEquipment}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class DashboardApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected EquipmentRepository equipmentRepository;
	@Autowired
	protected WarehouseRepository warehouseRepository;

	private long warehouseId;

	@BeforeEach
	void cleanAndSeed() {
		equipmentRepository.deleteAll();
		warehouseRepository.deleteAll();
		warehouseId = seedWarehouse();
	}

	private long seedWarehouse() {
		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo("ALM-DASH");
		warehouse.setNombre("Almacén Tablero");
		warehouse.setNombreNormalizado("ALMACEN TABLERO");
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
}
