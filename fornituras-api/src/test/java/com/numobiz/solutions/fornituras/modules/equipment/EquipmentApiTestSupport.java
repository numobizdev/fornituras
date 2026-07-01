package com.numobiz.solutions.fornituras.modules.equipment;

import com.numobiz.solutions.fornituras.modules.assignments.entity.Assignment;
import com.numobiz.solutions.fornituras.modules.assignments.repository.AssignmentRepository;
import com.numobiz.solutions.fornituras.modules.catalog.CatalogCodes;
import com.numobiz.solutions.fornituras.modules.catalog.entity.Catalog;
import com.numobiz.solutions.fornituras.modules.catalog.entity.CatalogItem;
import com.numobiz.solutions.fornituras.modules.catalog.repository.CatalogItemRepository;
import com.numobiz.solutions.fornituras.modules.catalog.repository.CatalogRepository;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.entity.EquipmentStatus;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import com.numobiz.solutions.fornituras.common.text.CodeNormalizer;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base común de las pruebas de contrato/integración del inventario de fornituras (ADR 0009): arranca
 * la aplicación completa sobre el perfil H2 en memoria ({@code application-test.yml}) con MockMvc y
 * seguridad real ({@code spring-security-test}). Antes de cada prueba deja la base limpia y siembra
 * los catálogos (TIPO_PRENDA/TALLA) y el almacén que el alta de fornituras exige, exponiendo sus id
 * vía {@link Seed}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class EquipmentApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected EquipmentRepository equipmentRepository;
	@Autowired
	protected AssignmentRepository assignmentRepository;
	@Autowired
	protected CatalogRepository catalogRepository;
	@Autowired
	protected CatalogItemRepository catalogItemRepository;
	@Autowired
	protected WarehouseRepository warehouseRepository;

	protected Seed seed;

	/** Referencias sembradas y reutilizables por cada prueba. */
	protected record Seed(long tipoPrendaId, long tallaId, long warehouseId) {
	}

	@BeforeEach
	void cleanAndSeed() {
		// Orden seguro para las FK que genera JPA (catalog_item → catalog).
		equipmentRepository.deleteAll();
		assignmentRepository.deleteAll();
		catalogItemRepository.deleteAll();
		catalogRepository.deleteAll();
		warehouseRepository.deleteAll();

		long tipoPrendaId = seedCatalogItem(CatalogCodes.TIPO_PRENDA, "Fornitura", true);
		long tallaId = seedCatalogItem(CatalogCodes.TALLA, "Mediana", true);
		long warehouseId = seedWarehouse();
		seed = new Seed(tipoPrendaId, tallaId, warehouseId);
	}

	private long seedCatalogItem(String catalogCode, String nombre, boolean active) {
		Catalog catalog = catalogRepository.findByCode(catalogCode).orElseGet(() -> {
			Catalog c = new Catalog();
			c.setCode(catalogCode);
			c.setNombre(catalogCode);
			c.setSystem(true);
			c.setActive(true);
			return catalogRepository.save(c);
		});
		CatalogItem item = new CatalogItem();
		item.setCatalog(catalog);
		item.setNombre(nombre);
		item.setNombreNormalizado(nombre.toUpperCase());
		item.setActive(active);
		return catalogItemRepository.save(item).getId();
	}

	protected long seedInactiveTipoPrenda() {
		return seedCatalogItem(CatalogCodes.TIPO_PRENDA, "Inactiva", false);
	}

	private long seedWarehouse() {
		Warehouse warehouse = new Warehouse();
		warehouse.setCodigo("ALM-01");
		warehouse.setNombre("Almacén Central");
		warehouse.setNombreNormalizado("ALMACEN CENTRAL");
		warehouse.setTipoItemId(1L);
		warehouse.setActive(true);
		return warehouseRepository.save(warehouse).getId();
	}

	/** Persiste una fornitura directamente (sin pasar por la API), con estado y vencimiento dados. */
	protected long persistEquipment(String codigo, EquipmentStatus status, LocalDate fechaVencimiento) {
		Equipment equipment = new Equipment();
		equipment.setCodigoQr(codigo.trim().toUpperCase());
		equipment.setCodigoNormalizado(CodeNormalizer.normalize(codigo));
		equipment.setEquipmentTypeId(seed.tipoPrendaId());
		equipment.setSizeId(seed.tallaId());
		equipment.setWarehouseId(seed.warehouseId());
		equipment.setStatus(status);
		equipment.setFechaVencimiento(fechaVencimiento);
		return equipmentRepository.save(equipment).getId();
	}

	/** Crea una asignación vigente (sin devolución) para comprometer la fornitura indicada. */
	protected void seedActiveAssignment(long equipmentId) {
		Assignment assignment = new Assignment();
		assignment.setEquipmentId(equipmentId);
		assignment.setOfficerId(1L);
		assignment.setFechaAsignacion(LocalDateTime.now());
		assignment.setFechaDevolucion(null);
		assignmentRepository.save(assignment);
	}

	/** JSON de alta individual con los datos generales sembrados y el código indicado. */
	protected String createJson(String codigo) throws Exception {
		return objectMapper.writeValueAsString(java.util.Map.of(
				"codigoQr", codigo,
				"equipmentTypeId", seed.tipoPrendaId(),
				"sizeId", seed.tallaId(),
				"warehouseId", seed.warehouseId(),
				"descripcion", "Chaleco de prueba"));
	}
}
