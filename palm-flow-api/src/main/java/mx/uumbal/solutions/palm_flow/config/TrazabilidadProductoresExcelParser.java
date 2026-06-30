package mx.uumbal.solutions.palm_flow.config;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TrazabilidadProductoresExcelParser {

    private static final String SHEET_NAME = "Trazabilidad 2026";

    private TrazabilidadProductoresExcelParser() {
    }

    record ProductorRow(String nombre, String centroAcopioNombre, String idAkk, String generoRaw) {
    }

    record TrazabilidadRow(
            String fiscalId,
            String plantation,
            String productorNombre,
            String idAkk,
            String generoRaw,
            Integer anioPlantacion,
            String comunidad,
            String municipio,
            String estado,
            String etapaRaw,
            String tipoPredioRaw,
            String actividadPrimariaRaw,
            String idGis,
            BigDecimal latitud,
            BigDecimal longitud,
            BigDecimal x,
            BigDecimal y,
            BigDecimal hectareas,
            String ramsarRaw,
            String anpRaw,
            String cambioRaw,
            String eudrRaw,
            String riesgoRaw,
            String wkt,
            String centroAcopioNombre
    ) {
    }

    record ProductoresPrediosSeedData(
            Map<String, ProductorRow> productoresByKey,
            List<TrazabilidadRow> rows
    ) {
    }

    static ProductoresPrediosSeedData parse(Path excelPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = ExcelSheetResolver.resolve(
                    workbook, SHEET_NAME, TrazabilidadProductoresExcelParser::hasHeaderColumns);

            HeaderColumns columns = findHeaderColumns(sheet);
            DataFormatter formatter = new DataFormatter();

            Map<String, ProductorRow> productoresByKey = new LinkedHashMap<>();
            List<TrazabilidadRow> rows = new ArrayList<>();

            for (int rowIndex = columns.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String plantation = normalize(formatter.formatCellValue(row.getCell(columns.plantationIndex())));
                if (plantation == null) {
                    continue;
                }

                String productorNombre = normalize(formatter.formatCellValue(row.getCell(columns.nomProdIndex())));
                String centroAcopioNombre = normalize(formatter.formatCellValue(row.getCell(columns.nomCaIndex())));
                if (productorNombre != null && centroAcopioNombre != null) {
                    String productorKey = productorKey(productorNombre, centroAcopioNombre);
                    productoresByKey.putIfAbsent(productorKey, new ProductorRow(
                            productorNombre,
                            centroAcopioNombre,
                            normalize(formatter.formatCellValue(row.getCell(columns.idAkkIndex()))),
                            normalize(formatter.formatCellValue(row.getCell(columns.generoIndex())))
                    ));
                }

                rows.add(new TrazabilidadRow(
                        TrazabilidadProductoresExcelParser.normalizeFiscalId(
                                formatter.formatCellValue(row.getCell(columns.fiscalIdIndex()))),
                        plantation,
                        productorNombre,
                        normalize(formatter.formatCellValue(row.getCell(columns.idAkkIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.generoIndex()))),
                        parseInteger(row.getCell(columns.plantYearIndex()), formatter),
                        normalize(formatter.formatCellValue(row.getCell(columns.comunidadIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.municipioIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.estadoIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.etapaIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.estatusIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.actPriIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.gisIndex()))),
                        parseDecimal(row.getCell(columns.latitudIndex()), formatter),
                        parseDecimal(row.getCell(columns.longitudIndex()), formatter),
                        parseDecimal(row.getCell(columns.xIndex()), formatter),
                        parseDecimal(row.getCell(columns.yIndex()), formatter),
                        parseDecimal(row.getCell(columns.hectareasIndex()), formatter),
                        normalize(formatter.formatCellValue(row.getCell(columns.ramsarIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.anpIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.cambioIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.eudrIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.riesgoIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.wktIndex()))),
                        centroAcopioNombre
                ));
            }

            return new ProductoresPrediosSeedData(productoresByKey, rows);
        }
    }

    static String productorKey(String nombre, String centroAcopioNombre) {
        return nombre.trim().toLowerCase() + "|" + centroAcopioNombre.trim().toLowerCase();
    }

    private static boolean hasHeaderColumns(Sheet sheet) {
        try {
            findHeaderColumns(sheet);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static HeaderColumns findHeaderColumns(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Map<String, Integer> headers = new LinkedHashMap<>();
            for (Cell cell : row) {
                String value = normalize(formatter.formatCellValue(cell));
                if (value != null) {
                    headers.putIfAbsent(value, cell.getColumnIndex());
                }
            }

            if (headers.containsKey("fiscal_id") && headers.containsKey("plantation")) {
                return new HeaderColumns(
                        rowIndex,
                        headers.get("plantation"),
                        headers.get("fiscal_id"),
                        headers.get("NOM_PROD_1"),
                        headers.get("ID_AKK"),
                        headers.get("GENERO"),
                        headers.get("plant_year"),
                        headers.get("COMUNIDAD"),
                        headers.get("MUNICIPIO"),
                        headers.get("ESTADO"),
                        headers.get("ETAPA_1"),
                        resolveIndex(headers, "ESTATUS_1", "ESTATUS__1"),
                        resolveIndex(headers, "ACT_PRI_1", "ACT_PRI__1"),
                        headers.get("GIS_1"),
                        headers.get("LATITUD"),
                        headers.get("LONGITUD"),
                        headers.get("X"),
                        headers.get("Y"),
                        headers.get("Hectareas"),
                        headers.get("RAMSAR"),
                        headers.get("ANP"),
                        headers.get("Cambio"),
                        headers.get("EUDR"),
                        headers.get("En_Riesgo"),
                        headers.get("WKT_Geometry"),
                        headers.get("NOM_CA_1")
                );
            }
        }

        throw new IllegalArgumentException("Header row with Trazabilidad productor/predio columns was not found");
    }

    private static int resolveIndex(Map<String, Integer> headers, String... candidates) {
        for (String candidate : candidates) {
            Integer index = headers.get(candidate);
            if (index != null) {
                return index;
            }
        }
        throw new IllegalArgumentException("Required column not found: " + String.join(" / ", candidates));
    }

    private static Integer parseInteger(Cell cell, DataFormatter formatter) {
        String text = normalize(formatter.formatCellValue(cell));
        if (text == null) {
            return null;
        }
        try {
            return Integer.parseInt(text.replace(".0", "").split("\\.")[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal parseDecimal(Cell cell, DataFormatter formatter) {
        if (cell == null) {
            return null;
        }
        String text = normalize(formatter.formatCellValue(cell));
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text.replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    static String normalizeFiscalId(String value) {
        return mx.uumbal.solutions.palm_flow.modules.productores.util.FiscalIdNormalizer.normalize(value);
    }

    private record HeaderColumns(
            int headerRowIndex,
            int plantationIndex,
            int fiscalIdIndex,
            int nomProdIndex,
            int idAkkIndex,
            int generoIndex,
            int plantYearIndex,
            int comunidadIndex,
            int municipioIndex,
            int estadoIndex,
            int etapaIndex,
            int estatusIndex,
            int actPriIndex,
            int gisIndex,
            int latitudIndex,
            int longitudIndex,
            int xIndex,
            int yIndex,
            int hectareasIndex,
            int ramsarIndex,
            int anpIndex,
            int cambioIndex,
            int eudrIndex,
            int riesgoIndex,
            int wktIndex,
            int nomCaIndex
    ) {
    }
}
