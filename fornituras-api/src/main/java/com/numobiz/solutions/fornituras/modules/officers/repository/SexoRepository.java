package com.numobiz.solutions.fornituras.modules.officers.repository;

import com.numobiz.solutions.fornituras.modules.officers.entity.Sexo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SexoRepository extends JpaRepository<Sexo, Long> {

	List<Sexo> findByActiveTrue();
}
