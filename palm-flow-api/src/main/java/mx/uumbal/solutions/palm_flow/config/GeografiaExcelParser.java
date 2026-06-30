package mx.uumbal.solutions.palm_flow.config;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class GeografiaExcelParser {

    private static final String SHEET_NAME = "Trazabilidad 2026";
    private static final String COL_ESTADO = "ESTADO";
    private static final String COL_MUNICIPIO = "MUNICIPIO";
    private static final String COL_COMUNIDAD = "COMUNIDAD";

    private GeografiaExcelParser() {
    }

    record GeografiaSeedData(
            Set<String> estados,
            Set<MunicipioKey> municipios,
            Set<ComunidadKey> comunidades
    ) {
    }

    record MunicipioKey(String estado, String municipio) {
    }

    record ComunidadKey(String estado, String municipio, String comunidad) {
    }

    static GeografiaSeedData parse(Path excelPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(excelPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = ExcelSheetResolver.resolve(workbook, SHEET_NAME, GeografiaExcelParser::hasHeaderColumns);

            HeaderColumns columns = findHeaderColumns(sheet);
            DataFormatter formatter = new DataFormatter();

            Set<String> estados = new LinkedHashSet<>();
            Set<MunicipioKey> municipios = new LinkedHashSet<>();
            Set<ComunidadKey> comunidades = new LinkedHashSet<>();

            for (int rowIndex = columns.headerRowIndex() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String estado = normalize(formatter.formatCellValue(row.getCell(columns.estadoIndex())));
                String municipio = normalize(formatter.formatCellValue(row.getCell(columns.municipioIndex())));
                String comunidad = normalize(formatter.formatCellValue(row.getCell(columns.comunidadIndex())));

                if (estado != null) {
                    estados.add(estado);
                }
                if (estado != null && municipio != null) {
                    municipios.add(new MunicipioKey(estado, municipio));
                }
                if (estado != null && municipio != null && comunidad != null) {
                    comunidades.add(new ComunidadKey(estado, municipio, comunidad));
                }
            }

            return new GeografiaSeedData(estados, municipios, comunidades);
        }
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
                    headers.put(value, cell.getColumnIndex());
                }
            }

            if (headers.containsKey(COL_ESTADO)
                    && headers.containsKey(COL_MUNICIPIO)
                    && headers.containsKey(COL_COMUNIDAD)) {
                return new HeaderColumns(
                        rowIndex,
                        headers.get(COL_ESTADO),
                        headers.get(COL_MUNICIPIO),
                        headers.get(COL_COMUNIDAD)
                );
            }
        }

        throw new IllegalArgumentException(
                "Header row with columns ESTADO, MUNICIPIO and COMUNIDAD was not found");
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record HeaderColumns(int headerRowIndex, int estadoIndex, int municipioIndex, int comunidadIndex) {
    }
}
