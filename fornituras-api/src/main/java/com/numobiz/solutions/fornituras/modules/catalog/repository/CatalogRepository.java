package com.numobiz.solutions.fornituras.modules.catalog.repository;

import com.numobiz.solutions.fornituras.modules.catalog.entity.Catalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CatalogRepository extends JpaRepository<Catalog, Long> {

	Optional<Catalog> findByCode(String code);

	List<Catalog> findByActiveTrueOrderByNombre();
}
