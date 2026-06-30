package com.numobiz.solutions.fornituras.modules.municipios.repository;

import com.numobiz.solutions.fornituras.modules.municipios.entity.Municipio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MunicipioRepository extends JpaRepository<Municipio, Long> {

	Optional<Municipio> findByNombreNormalizado(String nombreNormalizado);

	boolean existsByNombreNormalizado(String nombreNormalizado);

	Page<Municipio> findByActive(boolean active, Pageable pageable);
}
