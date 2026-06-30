package mx.uumbal.solutions.palm_flow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopio;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.CentroAcopioActivo;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.entity.Region;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.CentroAcopioRepository;
import mx.uumbal.solutions.palm_flow.modules.centros_acopio.repository.RegionRepository;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Comunidad;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Estado;
import mx.uumbal.solutions.palm_flow.modules.geografia.entity.Municipio;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.ComunidadRepository;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.EstadoRepository;
import mx.uumbal.solutions.palm_flow.modules.geografia.repository.MunicipioRepository;
import mx.uumbal.solutions.palm_flow.multitenancy.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seeds regiones and centros de acopio from the Acopios Excel catalog.
 * Idempotent: existing records matched by name are skipped.
 */
@Slf4j
@Component
@Order(6)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "palm-flow.seed.centros-acopio.enabled", havingValue = "true")
public class CentrosAcopioDataLoader implements ApplicationRunner {

    private static final String TENANT = EmpresaDataLoader.UUMBAL_SLUG;

    private final RegionRepository regionRepository;
    private final CentroAcopioRepository centroAcopioRepository;
    private final EstadoRepository estadoRepository;
    private final MunicipioRepository municipioRepository;
    private final ComunidadRepository comunidadRepository;

    @Value("${palm-flow.seed.centros-acopio.excel-path:docs/helper-files/Acopios Uumbal_junio2026.xlsx}")
    private String excelPath;

    @Value("${palm-flow.seed.centros-acopio.sheet-name:Acopios}")
    private String sheetName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Path path = ExcelSeedPathResolver.resolve(excelPath);
        if (!Files.isRegularFile(path)) {
            log.warn("Centros acopio seed skipped: Excel file not found at {}", path);
            return;
        }

        AcopiosExcelParser.AcopiosSeedData seedData;
        try {
            seedData = AcopiosExcelParser.parse(path, sheetName);
        } catch (Exception ex) {
            log.error("Centros acopio seed failed while reading {}: {}", path, ex.getMessage(), ex);
            return;
        }

        TenantContext.set(TENANT);
        try {
            Map<String, Region> regionesByNombre = seedRegiones(seedData);
            seedCentrosAcopio(seedData, regionesByNombre);
        } finally {
            TenantContext.clear();
        }
    }

    private Map<String, Region> seedRegiones(AcopiosExcelParser.AcopiosSeedData seedData) {
        Map<String, Region> regionesByNombre = new HashMap<>();
        int created = 0;

        for (String nombre : seedData.regiones()) {
            Region region = regionRepository.findByNombreIgnoreCase(nombre).orElse(null);
            if (region == null) {
                region = new Region();
                region.setNombre(nombre);
                region = regionRepository.save(region);
                created++;
            }
            regionesByNombre.put(nombre, region);
        }

        log.info("Centros acopio seed: {} distinct regiones ({} new)", seedData.regiones().size(), created);
        return regionesByNombre;
    }

    private void seedCentrosAcopio(
            AcopiosExcelParser.AcopiosSeedData seedData,
            Map<String, Region> regionesByNombre
    ) {
        int created = 0;
        List<String> skippedRows = new ArrayList<>();

        for (AcopiosExcelParser.CentroAcopioRow row : seedData.centros()) {
            if (centroAcopioRepository.findByNombreIgnoreCase(row.nombre()).isPresent()) {
                skippedRows.add(row.nombre() + ": already exists in database");
                continue;
            }

            Region region = regionesByNombre.get(row.region());
            if (region == null) {
                skippedRows.add(row.nombre() + ": region not found (" + row.region() + ")");
                continue;
            }

            String geografiaIssue = geografiaIssue(row);
            if (geografiaIssue != null) {
                skippedRows.add(row.nombre() + ": " + geografiaIssue);
                continue;
            }

            GeografiaRefs geografia = resolveGeografia(row);

            CentroAcopio centro = new CentroAcopio();
            centro.setNombre(row.nombre());
            centro.setRegion(region);
            centro.setActivo(mapActivo(row.activoRaw()));
            centro.setX(row.x());
            centro.setY(row.y());
            centro.setLatitud(row.latitud());
            centro.setLongitud(row.longitud());
            centro.setEncargado(row.encargado());
            centro.setEstado(geografia.estado());
            centro.setMunicipio(geografia.municipio());
            centro.setComunidad(geografia.comunidad());
            centro.setDistanciaKm(row.distanciaKm());
            centro.setAlias(row.alias());
            centroAcopioRepository.save(centro);
            created++;
        }

        log.info("Centros acopio seed: {} centros from Excel ({} new, {} skipped)",
                seedData.centros().size(), created, skippedRows.size());
        if (!skippedRows.isEmpty()) {
            log.warn("Centros acopio seed skipped rows ({}):", skippedRows.size());
            skippedRows.forEach(row -> log.warn("  - {}", row));
        }
    }

    private static String geografiaIssue(AcopiosExcelParser.CentroAcopioRow row) {
        List<String> missing = new ArrayList<>();
        if (row.estado() == null) {
            missing.add("estado");
        }
        if (row.municipio() == null) {
            missing.add("municipio");
        }
        if (row.comunidad() == null) {
            missing.add("comunidad");
        }
        if (missing.isEmpty()) {
            return null;
        }
        return "missing " + String.join("/", missing)
                + " (estado=" + valueOrDash(row.estado())
                + ", municipio=" + valueOrDash(row.municipio())
                + ", comunidad=" + valueOrDash(row.comunidad()) + ")";
    }

    private static String valueOrDash(String value) {
        return value == null ? "-" : value;
    }

    private GeografiaRefs resolveGeografia(AcopiosExcelParser.CentroAcopioRow row) {
        Estado estado = estadoRepository.findByNombreIgnoreCase(row.estado()).orElseGet(() -> {
            Estado entity = new Estado();
            entity.setNombre(row.estado());
            return estadoRepository.save(entity);
        });

        Municipio municipio = municipioRepository
                .findByNombreIgnoreCaseAndEstadoId(row.municipio(), estado.getId())
                .orElseGet(() -> {
                    Municipio entity = new Municipio();
                    entity.setNombre(row.municipio());
                    entity.setEstado(estado);
                    return municipioRepository.save(entity);
                });

        Comunidad comunidad = comunidadRepository
                .findByNombreIgnoreCaseAndMunicipioId(row.comunidad(), municipio.getId())
                .orElseGet(() -> {
                    Comunidad entity = new Comunidad();
                    entity.setNombre(row.comunidad());
                    entity.setMunicipio(municipio);
                    return comunidadRepository.save(entity);
                });

        return new GeografiaRefs(estado, municipio, comunidad);
    }

    private static CentroAcopioActivo mapActivo(String value) {
        if (value == null) {
            return CentroAcopioActivo.NO;
        }
        return switch (value.trim().toUpperCase()) {
            case "SI" -> CentroAcopioActivo.SI;
            case "EVENTUAL" -> CentroAcopioActivo.EVENTUAL;
            case "NO", "NO ACTIVO", "NA" -> CentroAcopioActivo.NO;
            default -> {
                log.warn("Unknown activo value '{}', defaulting to NO", value);
                yield CentroAcopioActivo.NO;
            }
        };
    }

    private record GeografiaRefs(Estado estado, Municipio municipio, Comunidad comunidad) {
    }
}
