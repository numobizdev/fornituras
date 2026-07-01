package com.numobiz.solutions.fornituras.modules.catalog.service;

import com.numobiz.solutions.fornituras.common.audit.AuditWriter;
import com.numobiz.solutions.fornituras.common.exception.BadRequestException;
import com.numobiz.solutions.fornituras.common.exception.ConflictException;
import com.numobiz.solutions.fornituras.common.exception.NotFoundException;
import com.numobiz.solutions.fornituras.common.text.NameNormalizer;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemCreateRequest;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogItemSummary;
import com.numobiz.solutions.fornituras.modules.catalog.dto.CatalogSummary;
import com.numobiz.solutions.fornituras.modules.catalog.entity.Catalog;
import com.numobiz.solutions.fornituras.modules.catalog.entity.CatalogItem;
import com.numobiz.solutions.fornituras.modules.catalog.mapper.CatalogMapper;
import com.numobiz.solutions.fornituras.modules.catalog.repository.CatalogItemRepository;
import com.numobiz.solutions.fornituras.modules.catalog.repository.CatalogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CRUD genérico de catálogos (ADR 0007). Un solo servicio administra los valores de cualquier
 * catálogo ({@code TIPO_PRENDA}, {@code TALLA}, {@code TIPO_ALMACEN}…): el catálogo se localiza
 * por su {@code code}. La unicidad de nombre se garantiza <b>dentro de cada catálogo</b> (dos
 * catálogos distintos pueden tener el mismo nombre de valor).
 *
 * <p>Expone además {@link #requireActiveItem} y {@link #resolveName} para que los módulos
 * consumidores (equipo, almacén) validen y muestren las referencias sin acoplarse a la entidad.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

	private final CatalogRepository catalogRepository;
	private final CatalogItemRepository itemRepository;
	private final CatalogMapper mapper;
	private final AuditWriter audit;

	public CatalogService(
			CatalogRepository catalogRepository,
			CatalogItemRepository itemRepository,
			CatalogMapper mapper,
			AuditWriter audit) {
		this.catalogRepository = catalogRepository;
		this.itemRepository = itemRepository;
		this.mapper = mapper;
		this.audit = audit;
	}

	public List<CatalogSummary> findCatalogs() {
		return mapper.toSummaryList(catalogRepository.findByActiveTrueOrderByNombre());
	}

	public Page<CatalogItemSummary> findItems(String catalogCode, Boolean active, Pageable pageable) {
		requireCatalog(catalogCode);
		Page<CatalogItem> page = (active == null)
				? itemRepository.findByCatalogCode(catalogCode, pageable)
				: itemRepository.findByCatalogCodeAndActive(catalogCode, active, pageable);
		return page.map(mapper::toItemSummary);
	}

	public CatalogItemSummary findItem(Long itemId) {
		return mapper.toItemSummary(getItemOrThrow(itemId));
	}

	/** Valores activos de un catálogo; si se indica {@code parentItemId}, acotados a ese padre. */
	public List<CatalogItemSummary> findActiveItems(String catalogCode, Long parentItemId) {
		requireCatalog(catalogCode);
		List<CatalogItem> items = (parentItemId == null)
				? itemRepository.findByCatalogCodeAndActiveTrueOrderByOrdenAscNombreAsc(catalogCode)
				: itemRepository.findByCatalogCodeAndParentItemIdAndActiveTrueOrderByOrdenAscNombreAsc(
						catalogCode, parentItemId);
		return mapper.toItemSummaryList(items);
	}

	@Transactional
	public CatalogItemSummary createItem(String catalogCode, CatalogItemCreateRequest request) {
		Catalog catalog = requireCatalog(catalogCode);
		String normalized = NameNormalizer.normalize(request.nombre());
		if (itemRepository.existsByCatalogIdAndNombreNormalizado(catalog.getId(), normalized)) {
			throw new ConflictException(
					"Ya existe un valor con el nombre '" + request.nombre() + "' en el catálogo " + catalogCode);
		}

		CatalogItem item = new CatalogItem();
		item.setCatalog(catalog);
		apply(item, request, normalized);
		item.setActive(true);

		CatalogItem saved = itemRepository.save(item);
		audit.record("CREATE_CATALOG_ITEM", saved.getId());
		return mapper.toItemSummary(saved);
	}

	@Transactional
	public CatalogItemSummary updateItem(Long itemId, CatalogItemCreateRequest request) {
		CatalogItem item = getItemOrThrow(itemId);
		String normalized = NameNormalizer.normalize(request.nombre());
		itemRepository.findByCatalogIdAndNombreNormalizado(item.getCatalog().getId(), normalized)
				.filter(existing -> !existing.getId().equals(itemId))
				.ifPresent(existing -> {
					throw new ConflictException("Ya existe un valor con el nombre '" + request.nombre()
							+ "' en el catálogo " + item.getCatalog().getCode());
				});

		apply(item, request, normalized);
		CatalogItem saved = itemRepository.save(item);
		audit.record("UPDATE_CATALOG_ITEM", saved.getId());
		return mapper.toItemSummary(saved);
	}

	/**
	 * Desactivación lógica (nunca borrado físico): un valor en uso por equipos/almacenes seguiría
	 * siendo referenciado, así que solo se marca inactivo y deja de ofrecerse en los selectores.
	 */
	@Transactional
	public void deactivateItem(Long itemId) {
		CatalogItem item = getItemOrThrow(itemId);
		item.setActive(false);
		itemRepository.save(item);
		audit.record("DEACTIVATE_CATALOG_ITEM", itemId);
	}

	/**
	 * Resuelve un valor validando que pertenezca al catálogo esperado y esté activo. Lo usan los
	 * consumidores (equipo, almacén) al validar sus referencias.
	 */
	public CatalogItem requireActiveItem(Long itemId, String expectedCatalogCode) {
		CatalogItem item = getItemOrThrow(itemId);
		if (!item.getCatalog().getCode().equals(expectedCatalogCode)) {
			throw new BadRequestException(
					"El valor " + itemId + " no pertenece al catálogo " + expectedCatalogCode);
		}
		if (!item.isActive()) {
			throw new BadRequestException("El valor de catálogo seleccionado está inactivo.");
		}
		return item;
	}

	/** Nombre del valor para mostrar (o {@code null} si no existe); no valida catálogo. */
	public String resolveName(Long itemId) {
		if (itemId == null) {
			return null;
		}
		return itemRepository.findById(itemId).map(CatalogItem::getNombre).orElse(null);
	}

	/** Resuelve en lote los nombres de varios valores (id → nombre); evita N+1 en listados. */
	public Map<Long, String> resolveNames(Collection<Long> itemIds) {
		if (itemIds == null || itemIds.isEmpty()) {
			return Map.of();
		}
		return itemRepository.findAllById(itemIds).stream()
				.collect(Collectors.toMap(CatalogItem::getId, CatalogItem::getNombre));
	}

	private void apply(CatalogItem item, CatalogItemCreateRequest request, String normalized) {
		item.setNombre(request.nombre().trim());
		item.setNombreNormalizado(normalized);
		item.setCode(blankToNull(request.code()));
		item.setDescripcion(request.descripcion());
		item.setFotoUrl(request.fotoUrl());
		item.setOrden(request.orden());
		item.setParentItem(resolveParent(request.parentItemId()));
	}

	private CatalogItem resolveParent(Long parentItemId) {
		if (parentItemId == null) {
			return null;
		}
		return getItemOrThrow(parentItemId);
	}

	private Catalog requireCatalog(String code) {
		return catalogRepository.findByCode(code)
				.orElseThrow(() -> new NotFoundException("Catálogo no encontrado: " + code));
	}

	private CatalogItem getItemOrThrow(Long itemId) {
		return itemRepository.findById(itemId)
				.orElseThrow(() -> new NotFoundException("Valor de catálogo no encontrado: " + itemId));
	}

	private String blankToNull(String value) {
		return (value == null || value.isBlank()) ? null : value.trim();
	}
}
