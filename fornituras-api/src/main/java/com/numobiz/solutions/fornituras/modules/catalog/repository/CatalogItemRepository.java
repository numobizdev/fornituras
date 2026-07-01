package com.numobiz.solutions.fornituras.modules.catalog.repository;

import com.numobiz.solutions.fornituras.modules.catalog.entity.CatalogItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {

	Page<CatalogItem> findByCatalogCode(String catalogCode, Pageable pageable);

	Page<CatalogItem> findByCatalogCodeAndActive(String catalogCode, boolean active, Pageable pageable);

	List<CatalogItem> findByCatalogCodeAndActiveTrueOrderByOrdenAscNombreAsc(String catalogCode);

	List<CatalogItem> findByCatalogCodeAndParentItemIdAndActiveTrueOrderByOrdenAscNombreAsc(
			String catalogCode, Long parentItemId);

	/**
	 * Unicidad de nombre para valores <b>globales</b> (sin padre): único por (catálogo, nombre) donde
	 * {@code parent_item_id IS NULL} (data-model 006, índice {@code uk_catalog_item_named}).
	 */
	Optional<CatalogItem> findByCatalogIdAndParentItemIsNullAndNombreNormalizado(
			Long catalogId, String nombreNormalizado);

	/**
	 * Unicidad de nombre para valores <b>dependientes</b>: único por (catálogo, padre, nombre), lo que
	 * permite el mismo nombre (p. ej. talla "M") bajo padres distintos (índice
	 * {@code uk_catalog_item_named_child}).
	 */
	Optional<CatalogItem> findByCatalogIdAndParentItemIdAndNombreNormalizado(
			Long catalogId, Long parentItemId, String nombreNormalizado);
}
