package com.numobiz.solutions.fornituras.modules.catalog;

import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemCreateRequest;
import com.numobiz.solutions.fornituras.modules.catalog.service.CatalogService;
import com.numobiz.solutions.fornituras.modules.equipment.entity.Equipment;
import com.numobiz.solutions.fornituras.modules.equipment.repository.EquipmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integración del catálogo genérico sobre H2 + JPA (T012): unicidad de nombre <b>por catálogo</b>,
 * resolución de valores <b>por {@code code}</b> (un catálogo no ve los valores de otro) y la regla
 * "no borrar en uso": la API no ofrece borrado físico; la desactivación conserva la fila para que
 * cualquier referencia existente ({@code equipment}) siga resolviéndose.
 */
class CatalogIntegrationTest extends CatalogApiTestSupport {

	@Autowired
	private CatalogService service;
	@Autowired
	private EquipmentRepository equipmentRepository;

	private CatalogItemCreateRequest request(String nombre) {
		return new CatalogItemCreateRequest(nombre, null, null, null, null, null);
	}

	@Test
	void nombreUniqueness_isScopedPerCatalog() {
		service.createItem(CatalogCodes.TIPO_PRENDA, request("Chaleco"));

		// Mismo nombre (normalizado) en el mismo catálogo → rechazado.
		assertThatThrownBy(() -> service.createItem(CatalogCodes.TIPO_PRENDA, request("  chaleco ")))
				.isInstanceOf(ConflictException.class);

		// Mismo nombre en OTRO catálogo → permitido (la unicidad es por catálogo).
		assertThat(service.createItem(CatalogCodes.TALLA, request("Chaleco"))).isNotNull();
	}

	@Test
	void findItems_resolvesPerCatalogCode() {
		service.createItem(CatalogCodes.TALLA, request("Mediana"));

		var tiposPrenda = service.findItems(CatalogCodes.TIPO_PRENDA, null, PageRequest.of(0, 10));
		assertThat(tiposPrenda.getContent()).extracting("nombre").containsExactly("Fornitura");

		var tallas = service.findItems(CatalogCodes.TALLA, null, PageRequest.of(0, 10));
		assertThat(tallas.getContent()).extracting("nombre").containsExactly("Mediana");
	}

	@Test
	void deactivate_keepsRowSoInUseReferenceSurvives() {
		long inUseItemId = seed.fornituraItemId();
		equipmentRepository.save(equipmentReferencing(inUseItemId));

		service.deactivateItem(inUseItemId);

		// La fila se conserva (integridad referencial): la referencia del equipo sigue resolviéndose.
		assertThat(itemRepository.findById(inUseItemId)).get()
				.extracting(item -> item.isActive()).isEqualTo(false);
		assertThat(service.resolveName(inUseItemId)).isEqualTo("Fornitura");
		assertThat(equipmentRepository.count()).isEqualTo(1);

		// Pero deja de ofrecerse como valor seleccionable.
		assertThat(service.findActiveItems(CatalogCodes.TIPO_PRENDA, null)).isEmpty();
		assertThatThrownBy(() -> service.requireActiveItem(inUseItemId, CatalogCodes.TIPO_PRENDA))
				.isInstanceOf(BadRequestException.class);
	}

	private Equipment equipmentReferencing(long tipoPrendaItemId) {
		Equipment equipment = new Equipment();
		equipment.setCodigoQr("FOR-USO-1");
		equipment.setCodigoNormalizado("FORUSO1");
		equipment.setEquipmentTypeId(tipoPrendaItemId);
		equipment.setWarehouseId(1L);
		return equipment;
	}
}
