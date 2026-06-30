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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AcopiosExcelParser {

    private static final String COL_LUGAR = "LUGAR";
    private static final String COL_ACTIVOS = "ACTIVOS 2026";
    private static final String COL_REGION = "REGION";
    private static final String COL_X = "X";
    private static final String COL_Y = "Y";
    private static final String COL_LATITUD = "LATITUD";
    private static final String COL_LONGITUD = "LONGITUD";
    private static final String COL_ENCARGADO = "ENCARGADO";
    private static final String COL_MUNICIPIO = "MUNICIPIO";
    private static final String COL_ESTADO = "ESTADO";
    private static final String COL_COMUNIDAD = "COMUNIDAD";
    private static final String COL_DISTANCIA = "Distancia (km)";
    private static final String COL_ALIAS = "MOSTRAR";

    private AcopiosExcelParser() {
    }

    record AcopiosSeedData(Set<String> regiones, List<CentroAcopioRow> centros) {
    }

    record CentroAcopioRow(
            String nombre,
            String activoRaw,
            String region,
            BigDecimal x,
            BigDecimal y,
            BigDecimal latitud,
            BigDecimal longitud,
            String encargado,
            String municipio,
            String estado,
            String comunidad,
            BigDecimal distanciaKm,
            String alias
    ) {
    }

    static AcopiosSeedData parse(Path excelPath, String preferredSheetName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = resolveSheet(workbook, preferredSheetName);
            HeaderColumns columns = findHeaderColumns(sheet);
            DataFormatter formatter = new DataFormatter();

            Set<String> regiones = new LinkedHashSet<>();
            List<CentroAcopioRow> centros = new ArrayList<>();

            for (int rowIndex = columns.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String nombre = normalize(formatter.formatCellValue(row.getCell(columns.lugarIndex())));
                if (nombre == null) {
                    continue;
                }

                String region = normalize(formatter.formatCellValue(row.getCell(columns.regionIndex())));
                if (region != null) {
                    regiones.add(region);
                }

                centros.add(new CentroAcopioRow(
                        nombre,
                        normalize(formatter.formatCellValue(row.getCell(columns.activosIndex()))),
                        region,
                        parseDecimal(row.getCell(columns.xIndex()), formatter),
                        parseDecimal(row.getCell(columns.yIndex()), formatter),
                        parseDecimal(row.getCell(columns.latitudIndex()), formatter),
                        parseDecimal(row.getCell(columns.longitudIndex()), formatter),
                        normalize(formatter.formatCellValue(row.getCell(columns.encargadoIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.municipioIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.estadoIndex()))),
                        normalize(formatter.formatCellValue(row.getCell(columns.comunidadIndex()))),
                        parseDecimal(row.getCell(columns.distanciaIndex()), formatter),
                        normalize(formatter.formatCellValue(row.getCell(columns.aliasIndex())))
                ));
            }

            return new AcopiosSeedData(regiones, centros);
        }
    }

    private static Sheet resolveSheet(Workbook workbook, String preferredSheetName) {
        return ExcelSheetResolver.resolve(workbook, preferredSheetName, AcopiosExcelParser::hasHeaderColumns);
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
                String value = normalizeHeader(formatter.formatCellValue(cell));
                if (value != null) {
                    headers.putIfAbsent(value, cell.getColumnIndex());
                }
            }

            if (hasRequiredHeaders(headers)) {
                return new HeaderColumns(
                        rowIndex,
                        headers.get(COL_LUGAR),
                        headers.get(COL_ACTIVOS),
                        headers.get(COL_REGION),
                        headers.get(COL_X),
                        headers.get(COL_Y),
                        headers.get(COL_LATITUD),
                        headers.get(COL_LONGITUD),
                        headers.get(COL_ENCARGADO),
                        headers.get(COL_MUNICIPIO),
                        headers.get(COL_ESTADO),
                        headers.get(COL_COMUNIDAD),
                        headers.get(COL_DISTANCIA),
                        headers.get(COL_ALIAS)
                );
            }
        }

        throw new IllegalArgumentException("Header row with Acopios columns was not found in sheet: " + sheet.getSheetName());
    }

    private static boolean hasRequiredHeaders(Map<String, Integer> headers) {
        return headers.containsKey(COL_LUGAR)
                && headers.containsKey(COL_ACTIVOS)
                && headers.containsKey(COL_REGION)
                && headers.containsKey(COL_X)
                && headers.containsKey(COL_Y)
                && headers.containsKey(COL_LATITUD)
                && headers.containsKey(COL_LONGITUD)
                && headers.containsKey(COL_ENCARGADO)
                && headers.containsKey(COL_MUNICIPIO)
                && headers.containsKey(COL_ESTADO)
                && headers.containsKey(COL_COMUNIDAD)
                && headers.containsKey(COL_DISTANCIA)
                && headers.containsKey(COL_ALIAS);
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

    private static String normalizeHeader(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record HeaderColumns(
            int headerRowIndex,
            int lugarIndex,
            int activosIndex,
            int regionIndex,
            int xIndex,
            int yIndex,
            int latitudIndex,
            int longitudIndex,
            int encargadoIndex,
            int municipioIndex,
            int estadoIndex,
            int comunidadIndex,
            int distanciaIndex,
            int aliasIndex
    ) {
    }
}
