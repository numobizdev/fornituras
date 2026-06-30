package com.numobiz.solutions.fornituras.modules.officers.repository;

import com.numobiz.solutions.fornituras.modules.officers.entity.TipoSangre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoSangreRepository extends JpaRepository<TipoSangre, Long> {

	List<TipoSangre> findByActiveTrue();
}
