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

	Optional<CatalogItem> findByCatalogIdAndNombreNormalizado(Long catalogId, String nombreNormalizado);

	boolean existsByCatalogIdAndNombreNormalizado(Long catalogId, String nombreNormalizado);
}
