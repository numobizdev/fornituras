package com.numobiz.solutions.fornituras.modules.landing.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionAdmin;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionCreateRequest;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionPublic;
import com.numobiz.solutions.fornituras.modules.landing.dto.LandingSectionUpdateRequest;
import com.numobiz.solutions.fornituras.modules.landing.dto.ReorderRequest;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingScope;
import com.numobiz.solutions.fornituras.modules.landing.entity.LandingSection;
import com.numobiz.solutions.fornituras.modules.landing.mapper.LandingSectionMapper;
import com.numobiz.solutions.fornituras.modules.landing.repository.LandingSectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Motor de contenido de la landing (feature 016): sirve las caras pública y de inicio (solo activas) y
 * permite al ADMIN administrar las secciones (alta, edición, baja lógica, reordenamiento). Todo el texto
 * se almacena literal (escape en el render) y las URLs se validan en el borde (anti-XSS, ADR 0015). Cada
 * escritura queda auditada por id, sin PII.
 */
@Service
@Transactional(readOnly = true)
public class LandingService {

	private final LandingSectionRepository repository;
	private final LandingSectionMapper mapper;
	private final AuditWriter audit;

	public LandingService(LandingSectionRepository repository, LandingSectionMapper mapper, AuditWriter audit) {
		this.repository = repository;
		this.mapper = mapper;
		this.audit = audit;
	}

	/** Cara pública (sin sesión): secciones PUBLIC activas ordenadas, sin PII. */
	public List<LandingSectionPublic> getPublic() {
		return activeOf(LandingScope.PUBLIC);
	}

	/** Home (autenticado): secciones HOME activas ordenadas. */
	public List<LandingSectionPublic> getHome() {
		return activeOf(LandingScope.HOME);
	}

	private List<LandingSectionPublic> activeOf(LandingScope scope) {
		return repository.findByScopeAndActiveTrueOrderByOrdenAsc(scope).stream()
				.map(mapper::toPublic)
				.toList();
	}

	/** Listado completo (incluye inactivas) para el editor de ADMIN. */
	public List<LandingSectionAdmin> list(LandingScope scope) {
		return repository.findByScopeOrderByOrdenAsc(scope).stream()
				.map(mapper::toAdmin)
				.toList();
	}

	@Transactional
	public LandingSectionAdmin create(LandingSectionCreateRequest request) {
		LandingSectionRules.validate(
				request.type(), request.titulo(), request.ctaLabel(), request.ctaUrl(), request.quickLinks());

		LandingSection section = new LandingSection();
		section.setScope(request.scope());
		section.setType(request.type());
		applyContent(section, request.titulo(), request.subtitulo(), request.cuerpo(),
				request.imagenUrl(), request.ctaLabel(), request.ctaUrl(), request.orden(),
				mapper.writeQuickLinks(request.quickLinks()));
		section.setActive(true);

		LandingSection saved = repository.save(section);
		audit.record("CREATE_LANDING_SECTION", saved.getId());
		return mapper.toAdmin(saved);
	}

	@Transactional
	public LandingSectionAdmin update(Long id, LandingSectionUpdateRequest request) {
		LandingSectionRules.validate(
				request.type(), request.titulo(), request.ctaLabel(), request.ctaUrl(), request.quickLinks());

		LandingSection section = getOrThrow(id);
		section.setScope(request.scope());
		section.setType(request.type());
		applyContent(section, request.titulo(), request.subtitulo(), request.cuerpo(),
				request.imagenUrl(), request.ctaLabel(), request.ctaUrl(), request.orden(),
				mapper.writeQuickLinks(request.quickLinks()));

		LandingSection saved = repository.save(section);
		audit.record("UPDATE_LANDING_SECTION", saved.getId());
		return mapper.toAdmin(saved);
	}

	@Transactional
	public LandingSectionAdmin deactivate(Long id) {
		return setActive(id, false, "DEACTIVATE_LANDING_SECTION");
	}

	@Transactional
	public LandingSectionAdmin activate(Long id) {
		return setActive(id, true, "ACTIVATE_LANDING_SECTION");
	}

	private LandingSectionAdmin setActive(Long id, boolean active, String action) {
		LandingSection section = getOrThrow(id);
		section.setActive(active);
		LandingSection saved = repository.save(section);
		audit.record(action, saved.getId());
		return mapper.toAdmin(saved);
	}

	@Transactional
	public List<LandingSectionAdmin> reorder(ReorderRequest request) {
		List<LandingSection> updated = request.items().stream()
				.map(item -> {
					LandingSection section = repository.findById(item.id())
							.orElseThrow(() -> new BadRequestException("Sección inexistente: " + item.id()));
					section.setOrden(item.orden());
					return repository.save(section);
				})
				.sorted(Comparator.comparingInt(LandingSection::getOrden))
				.toList();

		audit.recordEvent("REORDER_LANDING_SECTIONS", "count=" + updated.size());
		return updated.stream().map(mapper::toAdmin).toList();
	}

	private void applyContent(
			LandingSection section, String titulo, String subtitulo, String cuerpo,
			String imagenUrl, String ctaLabel, String ctaUrl, int orden, String configJson) {
		section.setTitulo(trimToNull(titulo));
		section.setSubtitulo(trimToNull(subtitulo));
		section.setCuerpo(trimToNull(cuerpo));
		section.setImagenUrl(trimToNull(imagenUrl));
		section.setCtaLabel(trimToNull(ctaLabel));
		section.setCtaUrl(trimToNull(ctaUrl));
		section.setOrden(orden);
		section.setConfigJson(configJson);
	}

	private LandingSection getOrThrow(Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new NotFoundException("Sección de landing no encontrada: " + id));
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
