package com.numobiz.solutions.fornituras.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Regla 3 del ADR 0013: solo ADMIN, SUPERVISOR y AUDITOR ven la PII completa (CURP/RFC en claro); los
 * roles operativos (CAPTURISTA) y de inventario (ALMACEN), así como el actor no autenticado, la ven
 * enmascarada. Centralizar la decisión evita que reportes y padrón diverjan.
 */
class RolePolicyTest {

	@Test
	void adminSupervisorAndAuditor_viewFullPii() {
		assertThat(RolePolicy.canViewFullPii(withRole("ROLE_ADMIN"))).isTrue();
		assertThat(RolePolicy.canViewFullPii(withRole("ROLE_SUPERVISOR"))).isTrue();
		assertThat(RolePolicy.canViewFullPii(withRole("ROLE_AUDITOR"))).isTrue();
	}

	@Test
	void operationalAndInventoryRoles_seeMaskedPii() {
		assertThat(RolePolicy.canViewFullPii(withRole("ROLE_CAPTURISTA"))).isFalse();
		assertThat(RolePolicy.canViewFullPii(withRole("ROLE_ALMACEN"))).isFalse();
	}

	@Test
	void unauthenticated_seesMaskedPii() {
		assertThat(RolePolicy.canViewFullPii(null)).isFalse();
	}

	private Authentication withRole(String authority) {
		return new UsernamePasswordAuthenticationToken(
				"tester", "n/a", List.of(new SimpleGrantedAuthority(authority)));
	}
}
