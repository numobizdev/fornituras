package com.numobiz.solutions.fornituras.modules.catalog;

import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemCreateRequest;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemSummary;
import com.numobiz.solutions.fornituras.modules.catalog.entity.Catalog;
import com.numobiz.solutions.fornituras.modules.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Catálogos dependientes item→item (T023, US2): una talla ({@code TALLA}) puede colgar de un tipo de
 * prenda ({@code TIPO_PRENDA}) vía {@code parentItemId}. La unicidad de nombre es <b>por (catálogo,
 * padre)</b>: el mismo nombre puede repetirse bajo padres distintos, pero no dentro del mismo padre.
 * Y una talla ligada a un tipo solo se ofrece para ese tipo.
 */
class CatalogHierarchyTest extends CatalogApiTestSupport {

	@Autowired
	private CatalogService service;

	private long cascoItemId() {
		Catalog tipoPrenda = catalogRepository.findById(seed.tipoPrendaCatalogId()).orElseThrow();
		return seedItem(tipoPrenda, "Casco", null, true).getId();
	}

	private CatalogItemCreateRequest talla(String nombre, Long parentItemId) {
		return new CatalogItemCreateRequest(nombre, null, null, null, parentItemId, null);
	}

	@Test
	void sameName_allowedUnderDifferentParents() {
		long fornitura = seed.fornituraItemId();
		long casco = cascoItemId();

		service.createItem(CatalogCodes.TALLA, talla("M", fornitura));
		CatalogItemSummary underCasco = service.createItem(CatalogCodes.TALLA, talla("M", casco));

		assertThat(underCasco).isNotNull();
		assertThat(service.findActiveItems(CatalogCodes.TALLA, null)).hasSize(2);
	}

	@Test
	void sameName_rejectedUnderSameParent() {
		long fornitura = seed.fornituraItemId();
		service.createItem(CatalogCodes.TALLA, talla("M", fornitura));

		assertThatThrownBy(() -> service.createItem(CatalogCodes.TALLA, talla("  m ", fornitura)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void globalAndChildUniqueness_areIndependent() {
		long fornitura = seed.fornituraItemId();

		service.createItem(CatalogCodes.TALLA, talla("M", null));      // global
		service.createItem(CatalogCodes.TALLA, talla("M", fornitura)); // hija: otro ámbito, permitido

		// Un segundo valor global "M" sí colisiona con el global existente.
		assertThatThrownBy(() -> service.createItem(CatalogCodes.TALLA, talla("M", null)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void sizeTiedToType_isOfferedOnlyForThatType() {
		long fornitura = seed.fornituraItemId();
		long casco = cascoItemId();
		service.createItem(CatalogCodes.TALLA, talla("M", fornitura));

		assertThat(service.findActiveItems(CatalogCodes.TALLA, fornitura))
				.extracting(CatalogItemSummary::nombre).containsExactly("M");
		assertThat(service.findActiveItems(CatalogCodes.TALLA, casco)).isEmpty();
	}
}
