package com.numobiz.solutions.fornituras.modules.landing.repository;

import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a las secciones de landing. La lectura de cara pública/home devuelve solo las activas de un
 * scope, ordenadas; el editor de ADMIN incluye también las inactivas.
 */
public interface LandingSectionRepository extends JpaRepository<LandingSection, Long> {

	/** Secciones visibles de una cara (solo activas), en orden de aparición. */
	List<LandingSection> findByScopeAndActiveTrueOrderByOrdenAsc(LandingScope scope);

	/** Todas las secciones de una cara (incluye inactivas) para el editor. */
	List<LandingSection> findByScopeOrderByOrdenAsc(LandingScope scope);
}
