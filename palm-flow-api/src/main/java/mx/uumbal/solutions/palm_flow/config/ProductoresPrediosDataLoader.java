package mx.uumbal.solutions.palm_flow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.CentroAcopioRepository;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.ComunidadRepository;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.EstadoRepository;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.MunicipioRepository;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.ActividadPrimaria;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.EtapaPredio;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Genero;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Lote;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Predio;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.Productor;
import mx.uumbal.solutions.palm_flow.modules.productores.entity.TipoPredio;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.LoteRepository;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.PredioRepository;
import mx.uumbal.solutions.palm_flow.modules.productores.repository.ProductorRepository;
import mx.uumbal.solutions.palm_flow.modules.productores.util.FiscalIdNormalizer;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.repository.RecepcionFrutaRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Seeds productores, predios and lotes from the Trazabilidad Excel catalog.
 * Productor key: NOM_PROD_1 + NOM_CA_1. Predio key: fiscal_id (or one predio per orphan row).
 */
@Slf4j
@Component
@Order(7)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "palm-flow.seed.productores-predios.enabled", havingValue = "true")
public class ProductoresPrediosDataLoader implements ApplicationRunner {

    private static final String TENANT = EmpresaDataLoader.UUMBAL_SLUG;

    private final ProductorRepository productorRepository;
    private final PredioRepository predioRepository;
    private final LoteRepository loteRepository;
    private final RecepcionFrutaRepository recepcionFrutaRepository;
    private final EstadoRepository estadoRepository;
    private final MunicipioRepository municipioRepository;
    private final ComunidadRepository comunidadRepository;
    private final CentroAcopioRepository centroAcopioRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${palm-flow.seed.productores-predios.excel-path:docs/helper-files/Trazabilidad_2026.xlsx}")
    private String excelPath;

    @Value("${palm-flow.seed.productores-predios.clear-existing:false}")
    private boolean clearExisting;

    @Value("${palm-flow.seed.productores-predios.batch-size:150}")
    private int batchSize;

    @Override
    public void run(ApplicationArguments args) {
        Path path = ExcelSeedPathResolver.resolve(excelPath);
        if (!Files.isRegularFile(path)) {
            log.warn("Productores/predios seed skipped: Excel file not found at {}", path);
            return;
        }

        TrazabilidadProductoresExcelParser.ProductoresPrediosSeedData seedData;
        try {
            seedData = TrazabilidadProductoresExcelParser.parse(path);
        } catch (Exception ex) {
            log.error("Productores/predios seed failed while reading {}: {}", path, ex.getMessage(), ex);
            return;
        }

        long startedAt = System.currentTimeMillis();
        TenantContext.set(TENANT);
        try {
            if (clearExisting) {
                transactionTemplate.executeWithoutResult(status -> clearExistingTenantData());
            }
            SeedContext context = transactionTemplate.execute(status -> buildSeedContext());

            Map<String, Productor> productoresByKey = transactionTemplate.execute(
                    status -> seedProductores(seedData, context));

            log.info("Predios/lotes seed: processing {} Excel rows in batches of {}",
                    seedData.rows().size(), batchSize);

            SeedStats stats = new SeedStats();
            List<TrazabilidadProductoresExcelParser.TrazabilidadRow> rows = seedData.rows();
            int totalBatches = (rows.size() + batchSize - 1) / batchSize;
            for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
                int from = batchIndex * batchSize;
                int to = Math.min(from + batchSize, rows.size());
                List<TrazabilidadProductoresExcelParser.TrazabilidadRow> batch = rows.subList(from, to);

                transactionTemplate.executeWithoutResult(status ->
                        processPrediosLotesBatch(batch, productoresByKey, context, stats));

                log.info(
                        "Predios/lotes seed progress: {}/{} rows (batch {}/{}, {} predios, {} lotes, {} skipped)",
                        to,
                        rows.size(),
                        batchIndex + 1,
                        totalBatches,
                        stats.prediosCreated,
                        stats.lotesCreated,
                        stats.skippedRows.size());
            }

            long elapsedMs = System.currentTimeMillis() - startedAt;
            log.info(
                    "Predios/lotes seed finished in {}s: {} rows ({} predios new, {} lotes new, {} skipped)",
                    elapsedMs / 1000,
                    rows.size(),
                    stats.prediosCreated,
                    stats.lotesCreated,
                    stats.skippedRows.size());

            if (!stats.skippedRows.isEmpty()) {
                log.warn("Predios/lotes seed skipped rows ({}):", stats.skippedRows.size());
                stats.skippedRows.stream().limit(50).forEach(row -> log.warn("  - {}", row));
                if (stats.skippedRows.size() > 50) {
                    log.warn("  ... and {} more skipped rows", stats.skippedRows.size() - 50);
                }
            }
        } finally {
            TenantContext.clear();
        }
    }

    private SeedContext buildSeedContext() {
        SeedContext context = new SeedContext();
        centroAcopioRepository.findAll().forEach(context::indexCentro);
        predioRepository.findByFiscalIdIsNotNull().forEach(predio -> {
            String key = FiscalIdNormalizer.key(predio.getFiscalId());
            if (key != null) {
                context.prediosByFiscalIdKey.put(key, predio);
            }
        });
        context.existingIdGisKeys.addAll(loteRepository.findDistinctIdGisLowerCase());
        estadoRepository.findAll().forEach(estado ->
                context.estadosByNombre.put(normalizeKey(estado.getNombre()), estado));
        municipioRepository.findAll().forEach(municipio ->
                context.municipiosByKey.put(municipioKey(municipio.getEstado().getId(), municipio.getNombre()), municipio));
        comunidadRepository.findAll().forEach(comunidad ->
                context.comunidadesByKey.put(
                        comunidadKey(comunidad.getMunicipio().getId(), comunidad.getNombre()), comunidad));
        log.info(
                "Predios/lotes seed caches: {} centros, {} predios by fiscal_id, {} id_gis, {} estados",
                context.centrosByLookupKey.size(),
                context.prediosByFiscalIdKey.size(),
                context.existingIdGisKeys.size(),
                context.estadosByNombre.size());
        return context;
    }

    private void clearExistingTenantData() {
        var recepciones = recepcionFrutaRepository.findAll();
        if (!recepciones.isEmpty()) {
            recepcionFrutaRepository.deleteAll(recepciones);
            log.warn("Productores/predios re-import: deleted {} recepciones_fruta for tenant {}",
                    recepciones.size(), TENANT);
        }

        var lotes = loteRepository.findAll();
        if (!lotes.isEmpty()) {
            loteRepository.deleteAll(lotes);
            log.warn("Productores/predios re-import: deleted {} lotes for tenant {}", lotes.size(), TENANT);
        }

        var predios = predioRepository.findAll();
        if (!predios.isEmpty()) {
            predioRepository.deleteAll(predios);
            log.warn("Productores/predios re-import: deleted {} predios for tenant {}", predios.size(), TENANT);
        }

        var productores = productorRepository.findAll();
        if (!productores.isEmpty()) {
            productorRepository.deleteAll(productores);
            log.warn("Productores/predios re-import: deleted {} productores for tenant {}",
                    productores.size(), TENANT);
        }
    }

    private Map<String, Productor> seedProductores(
            TrazabilidadProductoresExcelParser.ProductoresPrediosSeedData seedData,
            SeedContext context
    ) {
        Map<String, Productor> productoresByKey = new HashMap<>();
        int created = 0;

        for (Map.Entry<String, TrazabilidadProductoresExcelParser.ProductorRow> entry
                : seedData.productoresByKey().entrySet()) {
            TrazabilidadProductoresExcelParser.ProductorRow row = entry.getValue();
            CentroAcopio centroAcopio = context.resolveCentroAcopio(row.centroAcopioNombre());
            if (centroAcopio == null) {
                log.warn("Skipping productor {}: centro de acopio not found ({})",
                        row.nombre(), row.centroAcopioNombre());
                continue;
            }

            Productor productor = productorRepository
                    .findByNombreIgnoreCaseAndPredioCentroAcopioUuid(row.nombre(), centroAcopio.getUuid())
                    .orElse(null);
            if (productor == null) {
                productor = new Productor();
                productor.setNombre(row.nombre());
                productor.setIdAkk(row.idAkk());
                productor.setGenero(mapGenero(row.generoRaw()));
                productor.setActivo(true);
                productor = productorRepository.save(productor);
                created++;
            }
            productoresByKey.put(entry.getKey(), productor);
        }

        log.info("Productores seed: {} distinct productores by NOM_PROD_1+NOM_CA_1 ({} new)",
                productoresByKey.size(), created);
        return productoresByKey;
    }

    private void processPrediosLotesBatch(
            List<TrazabilidadProductoresExcelParser.TrazabilidadRow> batch,
            Map<String, Productor> productoresByKey,
            SeedContext context,
            SeedStats stats
    ) {
        for (TrazabilidadProductoresExcelParser.TrazabilidadRow row : batch) {
            if (row.productorNombre() == null || row.centroAcopioNombre() == null) {
                stats.skippedRows.add(row.plantation() + ": missing NOM_PROD_1 or NOM_CA_1");
                continue;
            }

            String productorKey = TrazabilidadProductoresExcelParser.productorKey(
                    row.productorNombre(), row.centroAcopioNombre());
            Productor productor = productoresByKey.get(productorKey);
            if (productor == null) {
                stats.skippedRows.add(row.plantation() + ": productor not found for " + productorKey);
                continue;
            }

            CentroAcopio centroAcopio = context.resolveCentroAcopio(row.centroAcopioNombre());
            if (centroAcopio == null) {
                stats.skippedRows.add(row.plantation() + ": centro de acopio not found");
                continue;
            }

            if (context.isLoteAlreadyKnown(row.idGis())) {
                stats.skippedRows.add(row.plantation() + ": lote already exists (id_gis=" + row.idGis() + ")");
                continue;
            }

            Predio predio = resolvePredio(row, productor, centroAcopio, context);
            if (predio == null) {
                stats.skippedRows.add(row.plantation() + ": could not resolve predio");
                continue;
            }
            if (predio.getUuid() == null) {
                predio = predioRepository.save(predio);
                stats.prediosCreated++;
                String fiscalIdKey = FiscalIdNormalizer.key(row.fiscalId());
                if (fiscalIdKey != null) {
                    context.prediosByFiscalIdKey.put(fiscalIdKey, predio);
                }
            }

            Lote lote = buildLote(row, predio);
            loteRepository.save(lote);
            context.registerNewLote(row.idGis());
            stats.lotesCreated++;
        }
    }

    private Predio resolvePredio(
            TrazabilidadProductoresExcelParser.TrazabilidadRow row,
            Productor productor,
            CentroAcopio centroAcopio,
            SeedContext context
    ) {
        String fiscalIdKey = FiscalIdNormalizer.key(row.fiscalId());
        if (fiscalIdKey != null) {
            Predio existing = context.prediosByFiscalIdKey.get(fiscalIdKey);
            if (existing != null) {
                return existing;
            }
            existing = predioRepository.findByFiscalIdIgnoreCase(row.fiscalId()).orElse(null);
            if (existing != null) {
                context.prediosByFiscalIdKey.put(fiscalIdKey, existing);
                return existing;
            }
            context.predioNombreByFiscalIdKey.putIfAbsent(fiscalIdKey, row.plantation());
            Predio predio = buildPredio(
                    context.predioNombreByFiscalIdKey.get(fiscalIdKey),
                    row.fiscalId(),
                    row,
                    productor,
                    centroAcopio,
                    context);
            context.prediosByFiscalIdKey.put(fiscalIdKey, predio);
            return predio;
        }

        if (row.idGis() != null) {
            String idGisKey = normalizeKey(row.idGis());
            Predio cached = context.prediosByIdGisKey.get(idGisKey);
            if (cached != null) {
                return cached;
            }
            return loteRepository.findByIdGisIgnoreCase(row.idGis())
                    .map(lote -> {
                        context.prediosByIdGisKey.put(idGisKey, lote.getPredio());
                        return lote.getPredio();
                    })
                    .orElseGet(() -> buildPredio(row.plantation(), null, row, productor, centroAcopio, context));
        }

        return buildPredio(row.plantation(), null, row, productor, centroAcopio, context);
    }

    private Predio buildPredio(
            String nombre,
            String fiscalId,
            TrazabilidadProductoresExcelParser.TrazabilidadRow row,
            Productor productor,
            CentroAcopio centroAcopio,
            SeedContext context
    ) {
        GeografiaRefs geo = resolveGeografia(row, context);
        Predio predio = new Predio();
        predio.setNombre(nombre);
        predio.setFiscalId(FiscalIdNormalizer.normalize(fiscalId));
        predio.setProductor(productor);
        predio.setCentroAcopio(centroAcopio);
        predio.setEstado(geo.estado());
        predio.setMunicipio(geo.municipio());
        predio.setComunidad(geo.comunidad());
        predio.setTipoPredio(mapTipoPredio(row.tipoPredioRaw()));
        predio.setActividadPrimaria(mapActividadPrimaria(row.actividadPrimariaRaw()));
        return predio;
    }

    private Lote buildLote(TrazabilidadProductoresExcelParser.TrazabilidadRow row, Predio predio) {
        Lote lote = new Lote();
        lote.setPredio(predio);
        lote.setNombre(row.plantation());
        lote.setAnioPlantacion(row.anioPlantacion());
        lote.setEtapa(mapEtapa(row.etapaRaw()));
        lote.setIdGis(row.idGis());
        lote.setLatitud(row.latitud());
        lote.setLongitud(row.longitud());
        lote.setX(row.x());
        lote.setY(row.y());
        lote.setHectareas(row.hectareas());
        lote.setRamsar(parseBoolean(row.ramsarRaw()));
        lote.setAnp(parseBoolean(row.anpRaw()));
        lote.setCambio(parseBoolean(row.cambioRaw()));
        lote.setEudr(parseInteger(row.eudrRaw(), 0));
        lote.setRiesgo(parseInteger(row.riesgoRaw(), 0));
        lote.setWkt(row.wkt());
        return lote;
    }

    private GeografiaRefs resolveGeografia(
            TrazabilidadProductoresExcelParser.TrazabilidadRow row,
            SeedContext context
    ) {
        if (row.estado() == null) {
            return new GeografiaRefs(null, null, null);
        }

        String estadoKey = normalizeKey(row.estado());
        Estado estado = context.estadosByNombre.get(estadoKey);
        if (estado == null) {
            estado = estadoRepository.findByNombreIgnoreCase(row.estado()).orElse(null);
            if (estado == null) {
                Estado entity = new Estado();
                entity.setNombre(row.estado());
                estado = estadoRepository.save(entity);
            }
            context.estadosByNombre.put(estadoKey, estado);
        }

        if (row.municipio() == null) {
            return new GeografiaRefs(estado, null, null);
        }

        String municipioKey = municipioKey(estado.getId(), row.municipio());
        Municipio municipio = context.municipiosByKey.get(municipioKey);
        if (municipio == null) {
            municipio = municipioRepository
                    .findByNombreIgnoreCaseAndEstadoId(row.municipio(), estado.getId())
                    .orElse(null);
            if (municipio == null) {
                Municipio entity = new Municipio();
                entity.setNombre(row.municipio());
                entity.setEstado(estado);
                municipio = municipioRepository.save(entity);
            }
            context.municipiosByKey.put(municipioKey, municipio);
        }

        if (row.comunidad() == null) {
            return new GeografiaRefs(estado, municipio, null);
        }

        String comunidadKey = comunidadKey(municipio.getId(), row.comunidad());
        Comunidad comunidad = context.comunidadesByKey.get(comunidadKey);
        if (comunidad == null) {
            comunidad = comunidadRepository
                    .findByNombreIgnoreCaseAndMunicipioId(row.comunidad(), municipio.getId())
                    .orElse(null);
            if (comunidad == null) {
                Comunidad entity = new Comunidad();
                entity.setNombre(row.comunidad());
                entity.setMunicipio(municipio);
                comunidad = comunidadRepository.save(entity);
            }
            context.comunidadesByKey.put(comunidadKey, comunidad);
        }

        return new GeografiaRefs(estado, municipio, comunidad);
    }

    private static String normalizeKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String municipioKey(Long estadoId, String municipioNombre) {
        return estadoId + "|" + normalizeKey(municipioNombre);
    }

    private static String comunidadKey(Long municipioId, String comunidadNombre) {
        return municipioId + "|" + normalizeKey(comunidadNombre);
    }

    private static Genero mapGenero(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase()) {
            case "M" -> Genero.M;
            case "F" -> Genero.F;
            default -> null;
        };
    }

    private static EtapaPredio mapEtapa(String value) {
        if (value == null) {
            return null;
        }
        try {
            return EtapaPredio.valueOf(normalizeEnum(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static TipoPredio mapTipoPredio(String value) {
        if (value == null) {
            return null;
        }
        String normalized = normalizeEnum(value);
        if ("PEQUENA PROPIEDAD".equals(normalized)) {
            return TipoPredio.PEQUENA_PROPIEDAD;
        }
        try {
            return TipoPredio.valueOf(normalized.replace(' ', '_'));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ActividadPrimaria mapActividadPrimaria(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ActividadPrimaria.valueOf(normalizeEnum(value));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String normalizeEnum(String value) {
        String withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toUpperCase();
    }

    private static boolean parseBoolean(String value) {
        if (value == null) {
            return false;
        }
        return switch (value.trim().toUpperCase()) {
            case "1", "SI", "TRUE", "YES" -> true;
            default -> false;
        };
    }

    private static int parseInteger(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim().split("\\.")[0]);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static final class SeedContext {
        final Map<String, CentroAcopio> centrosByLookupKey = new HashMap<>();
        final Map<String, Predio> prediosByFiscalIdKey = new HashMap<>();
        final Map<String, String> predioNombreByFiscalIdKey = new HashMap<>();
        final Map<String, Predio> prediosByIdGisKey = new HashMap<>();
        final Set<String> existingIdGisKeys = new HashSet<>();
        final Map<String, Estado> estadosByNombre = new HashMap<>();
        final Map<String, Municipio> municipiosByKey = new HashMap<>();
        final Map<String, Comunidad> comunidadesByKey = new HashMap<>();

        void indexCentro(CentroAcopio centro) {
            if (centro.getNombre() != null) {
                centrosByLookupKey.put(normalizeKey(centro.getNombre()), centro);
            }
            if (centro.getAlias() != null) {
                centrosByLookupKey.put(normalizeKey(centro.getAlias()), centro);
            }
        }

        CentroAcopio resolveCentroAcopio(String nombre) {
            if (nombre == null) {
                return null;
            }
            return centrosByLookupKey.get(normalizeKey(nombre));
        }

        boolean isLoteAlreadyKnown(String idGis) {
            if (idGis == null) {
                return false;
            }
            return existingIdGisKeys.contains(normalizeKey(idGis));
        }

        void registerNewLote(String idGis) {
            if (idGis != null) {
                existingIdGisKeys.add(normalizeKey(idGis));
            }
        }
    }

    private static final class SeedStats {
        int prediosCreated;
        int lotesCreated;
        final List<String> skippedRows = new ArrayList<>();
    }

    private record GeografiaRefs(Estado estado, Municipio municipio, Comunidad comunidad) {
    }
}
