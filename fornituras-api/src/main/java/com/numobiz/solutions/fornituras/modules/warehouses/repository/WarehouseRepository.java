package com.numobiz.solutions.fornituras.modules.warehouses.repository;

import com.numobiz.solutions.fornituras.modules.warehouses.entity.Warehouse;
import com.numobiz.solutions.fornituras.modules.warehouses.entity.WarehouseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

	boolean existsByCodigoIgnoreCase(String codigo);

	Optional<Warehouse> findByCodigoIgnoreCase(String codigo);

	boolean existsByNombreNormalizado(String nombreNormalizado);

	Optional<Warehouse> findByNombreNormalizado(String nombreNormalizado);

	Page<Warehouse> findByActive(boolean active, Pageable pageable);

	Page<Warehouse> findByTipo(WarehouseType tipo, Pageable pageable);

	Page<Warehouse> findByActiveAndTipo(boolean active, WarehouseType tipo, Pageable pageable);
}
