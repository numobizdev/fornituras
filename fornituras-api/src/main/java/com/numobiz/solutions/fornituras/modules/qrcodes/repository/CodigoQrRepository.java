package com.numobiz.solutions.fornituras.modules.qrcodes.repository;

import com.numobiz.solutions.fornituras.modules.qrcodes.entity.CodigoQR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface CodigoQrRepository extends JpaRepository<CodigoQR, Long> {

	boolean existsByCodigo(String codigo);

	@Query("SELECT c.codigo FROM CodigoQR c WHERE c.codigo IN :codigos")
	Set<String> findExistingCodigosIn(@Param("codigos") Collection<String> codigos);

	List<CodigoQR> findByLoteQrIdOrderByCodigoAsc(Long loteQrId);
}
