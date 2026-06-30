package com.numobiz.solutions.fornituras.modules.equipmenttypes.repository;

import com.numobiz.solutions.fornituras.modules.equipmenttypes.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SizeRepository extends JpaRepository<Size, Long> {

	List<Size> findByActiveTrue();

	List<Size> findByEquipmentTypeIdAndActiveTrue(Long equipmentTypeId);

	boolean existsByEquipmentTypeId(Long equipmentTypeId);
}
