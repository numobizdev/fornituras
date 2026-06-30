package com.numobiz.solutions.fornituras.modules.qrcodes.repository;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.LoteQR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoteQrRepository extends JpaRepository<LoteQR, Long> {

	List<LoteQR> findAllByOrderByCreatedAtDesc();

	@Query(value = "SELECT COALESCE(MAX(consecutivo_final), 0) FROM lote_qr WITH (UPDLOCK, HOLDLOCK)", nativeQuery = true)
	int findMaxConsecutivoFinalForUpdate();
}
