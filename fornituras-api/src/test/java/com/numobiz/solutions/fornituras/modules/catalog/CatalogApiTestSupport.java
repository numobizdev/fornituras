package com.numobiz.solutions.fornituras.modules.catalog;

import com.numobiz.solutions.fornituras.common.text.NameNormalizer;
import com.numobiz.solutions.fornituras.modules.catalog.entity.Catalog;
import com.numobiz.solutions.fornituras.modules.catalog.entity.CatalogItem;
import com.numobiz.solutions.fornituras.modules.catalog.repository.CatalogItemRepository;
import com.numobiz.solutions.fornituras.modules.catalog.repository.CatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * Base común de las pruebas de contrato/integración del catálogo genérico (feature 006): arranca la
 * aplicación completa sobre el perfil H2 en memoria ({@code application-test.yml}) con MockMvc y
 * seguridad real ({@code spring-security-test}). Antes de cada prueba deja las tablas de catálogo
 * limpias y siembra los catálogos de sistema {@code TIPO_PRENDA} (con el valor "Fornitura") y
 * {@code TALLA}, exponiendo sus id vía {@link Seed}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class CatalogApiTestSupport {

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected ObjectMapper objectMapper;
	@Autowired
	protected CatalogRepository catalogRepository;
	@Autowired
	protected CatalogItemRepository itemRepository;

	protected Seed seed;

	/** Referencias sembradas y reutilizables por cada prueba. */
	protected record Seed(long tipoPrendaCatalogId, long tallaCatalogId, long fornituraItemId) {
	}

	@BeforeEach
	void cleanAndSeed() {
		// Orden seguro para la FK que genera JPA (catalog_item → catalog / self parent).
		itemRepository.deleteAll();
		catalogRepository.deleteAll();

		Catalog tipoPrenda = seedCatalog(CatalogCodes.TIPO_PRENDA, "Tipo de prenda");
		Catalog talla = seedCatalog(CatalogCodes.TALLA, "Tallas");
		long fornituraItemId = seedItem(tipoPrenda, "Fornitura", null, true).getId();
		seed = new Seed(tipoPrenda.getId(), talla.getId(), fornituraItemId);
	}

	protected Catalog seedCatalog(String code, String nombre) {
		Catalog catalog = new Catalog();
		catalog.setCode(code);
		catalog.setNombre(nombre);
		catalog.setSystem(true);
		catalog.setActive(true);
		return catalogRepository.save(catalog);
	}

	protected CatalogItem seedItem(Catalog catalog, String nombre, CatalogItem parent, boolean active) {
		CatalogItem item = new CatalogItem();
		item.setCatalog(catalog);
		item.setNombre(nombre);
		item.setNombreNormalizado(NameNormalizer.normalize(nombre));
		item.setParentItem(parent);
		item.setActive(active);
		return itemRepository.save(item);
	}

	/** JSON mínimo de alta/edición: solo el nombre (el resto de campos son opcionales). */
	protected String itemJson(String nombre) throws Exception {
		return objectMapper.writeValueAsString(Map.of("nombre", nombre));
	}
}
