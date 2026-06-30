package com.numobiz.solutions.fornituras.modules.equipmenttypes.repository;

import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.EquipmentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EquipmentTypeRepository extends JpaRepository<EquipmentType, Long> {

	Optional<EquipmentType> findByNombreNormalizado(String nombreNormalizado);

	boolean existsByNombreNormalizado(String nombreNormalizado);

	Page<EquipmentType> findByActive(boolean active, Pageable pageable);
}
