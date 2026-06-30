package com.numobiz.solutions.fornituras.modules.qrcodes.repository;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoteQrRepository extends JpaRepository<LoteQR, Long> {

	List<LoteQR> findAllByOrderByCreatedAtDesc();
}
