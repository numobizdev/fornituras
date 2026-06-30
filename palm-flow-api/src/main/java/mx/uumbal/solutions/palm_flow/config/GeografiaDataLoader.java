package mx.uumbal.solutions.palm_flow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Seeds estados, municipios and comunidades from the Trazabilidad Excel catalog.
 * Idempotent: existing records matched by name (and parent FK) are skipped.
 */
@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "palm-flow.seed.geografia.enabled", havingValue = "true")
public class GeografiaDataLoader implements ApplicationRunner {

    private static final String TENANT = EmpresaDataLoader.UUMBAL_SLUG;

    private final EstadoRepository estadoRepository;
    private final MunicipioRepository municipioRepository;
    private final ComunidadRepository comunidadRepository;

    @Value("${palm-flow.seed.geografia.excel-path:docs/helper-files/Trazabilidad_2026.xlsx}")
    private String excelPath;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Path path = ExcelSeedPathResolver.resolve(excelPath);
        if (!Files.isRegularFile(path)) {
            log.warn("Geografia seed skipped: Excel file not found at {}", path);
            return;
        }

        GeografiaExcelParser.GeografiaSeedData seedData;
        try {
            seedData = GeografiaExcelParser.parse(path);
        } catch (Exception ex) {
            log.error("Geografia seed failed while reading {}: {}", path, ex.getMessage(), ex);
            return;
        }

        TenantContext.set(TENANT);
        try {
            Map<String, Estado> estadosByNombre = seedEstados(seedData);
            Map<GeografiaExcelParser.MunicipioKey, Municipio> municipiosByKey =
                    seedMunicipios(seedData, estadosByNombre);
            seedComunidades(seedData, municipiosByKey);
        } finally {
            TenantContext.clear();
        }
    }

    private Map<String, Estado> seedEstados(GeografiaExcelParser.GeografiaSeedData seedData) {
        Map<String, Estado> estadosByNombre = new HashMap<>();
        int created = 0;

        for (String nombre : seedData.estados()) {
            Estado estado = estadoRepository.findByNombreIgnoreCase(nombre).orElse(null);
            if (estado == null) {
                estado = new Estado();
                estado.setNombre(nombre);
                estado = estadoRepository.save(estado);
                created++;
            }
            estadosByNombre.put(nombre, estado);
        }

        log.info("Geografia seed: {} distinct estados ({} new)", seedData.estados().size(), created);
        return estadosByNombre;
    }

    private Map<GeografiaExcelParser.MunicipioKey, Municipio> seedMunicipios(
            GeografiaExcelParser.GeografiaSeedData seedData,
            Map<String, Estado> estadosByNombre
    ) {
        Map<GeografiaExcelParser.MunicipioKey, Municipio> municipiosByKey = new HashMap<>();
        int created = 0;

        for (GeografiaExcelParser.MunicipioKey key : seedData.municipios()) {
            Estado estado = estadosByNombre.get(key.estado());
            if (estado == null) {
                log.warn("Skipping municipio {}: estado {} not found", key.municipio(), key.estado());
                continue;
            }

            Municipio municipio = municipioRepository
                    .findByNombreIgnoreCaseAndEstadoId(key.municipio(), estado.getId())
                    .orElse(null);
            if (municipio == null) {
                municipio = new Municipio();
                municipio.setNombre(key.municipio());
                municipio.setEstado(estado);
                municipio = municipioRepository.save(municipio);
                created++;
            }

            municipiosByKey.put(key, municipio);
        }

        log.info("Geografia seed: {} distinct municipios ({} new)", seedData.municipios().size(), created);
        return municipiosByKey;
    }

    private void seedComunidades(
            GeografiaExcelParser.GeografiaSeedData seedData,
            Map<GeografiaExcelParser.MunicipioKey, Municipio> municipiosByKey
    ) {
        int created = 0;

        for (GeografiaExcelParser.ComunidadKey key : seedData.comunidades()) {
            GeografiaExcelParser.MunicipioKey municipioKey =
                    new GeografiaExcelParser.MunicipioKey(key.estado(), key.municipio());
            Municipio municipio = municipiosByKey.get(municipioKey);
            if (municipio == null) {
                log.warn("Skipping comunidad {}: municipio {} / {} not found",
                        key.comunidad(), key.municipio(), key.estado());
                continue;
            }

            boolean exists = comunidadRepository
                    .findByNombreIgnoreCaseAndMunicipioId(key.comunidad(), municipio.getId())
                    .isPresent();
            if (!exists) {
                Comunidad comunidad = new Comunidad();
                comunidad.setNombre(key.comunidad());
                comunidad.setMunicipio(municipio);
                comunidadRepository.save(comunidad);
                created++;
            }
        }

        log.info("Geografia seed: {} distinct comunidades ({} new)", seedData.comunidades().size(), created);
    }
}
