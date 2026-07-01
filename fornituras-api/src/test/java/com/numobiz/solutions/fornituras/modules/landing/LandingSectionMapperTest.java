package com.numobiz.solutions.fornituras.modules.landing;

import com.numobiz.solutions.fornituras.modules.landing.dto.QuickLinkItem;
import com.numobiz.solutions.fornituras.modules.landing.dto.SafeUrlValidator;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSection;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSectionType;
import com.numobiz.solutions.fornituras.modules.landing.mapper.LandingSectionMapper;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias del mapeo de accesos rápidos (serialización a/desde {@code config_json}) y del
 * validador de esquema de URL segura (anti-XSS). No arrancan el contexto de Spring.
 */
class LandingSectionMapperTest {

	private final LandingSectionMapper mapper = new LandingSectionMapper(JsonMapper.builder().build());
	private final SafeUrlValidator urlValidator = new SafeUrlValidator();

	@Test
	void quickLinks_roundTripsThroughConfigJson() {
		List<QuickLinkItem> links = List.of(
				new QuickLinkItem("Elementos", "/elementos", "people-outline"),
				new QuickLinkItem("Sitio", "https://gob.mx", null));

		String json = mapper.writeQuickLinks(links);
		assertThat(json).isNotNull();

		LandingSection section = new LandingSection();
		section.setScope(LandingScope.HOME);
		section.setType(LandingSectionType.QUICK_LINKS);
		section.setConfigJson(json);

		List<QuickLinkItem> restored = mapper.toPublic(section).quickLinks();
		assertThat(restored).containsExactlyElementsOf(links);
	}

	@Test
	void writeQuickLinks_emptyOrNull_returnsNull() {
		assertThat(mapper.writeQuickLinks(null)).isNull();
		assertThat(mapper.writeQuickLinks(List.of())).isNull();
	}

	@Test
	void safeUrl_acceptsInternalAndHttpSchemes() {
		assertThat(urlValidator.isValid("/login", null)).isTrue();
		assertThat(urlValidator.isValid("http://gob.mx", null)).isTrue();
		assertThat(urlValidator.isValid("https://gob.mx/x", null)).isTrue();
		assertThat(urlValidator.isValid(null, null)).isTrue();
		assertThat(urlValidator.isValid("  ", null)).isTrue();
	}

	@Test
	void safeUrl_rejectsDangerousSchemes() {
		assertThat(urlValidator.isValid("javascript:alert(1)", null)).isFalse();
		assertThat(urlValidator.isValid("data:text/html,x", null)).isFalse();
		assertThat(urlValidator.isValid("//evil.example", null)).isFalse();
		assertThat(urlValidator.isValid("ftp://x", null)).isFalse();
	}
}
