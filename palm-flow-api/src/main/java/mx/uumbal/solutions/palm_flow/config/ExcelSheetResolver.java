package mx.uumbal.solutions.palm_flow.config;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

final class ExcelSheetResolver {

    private ExcelSheetResolver() {
    }

    static Sheet resolve(Workbook workbook, String preferredSheetName, SheetHeaderProbe headerProbe) {
        if (preferredSheetName != null && !preferredSheetName.isBlank()) {
            Sheet preferred = workbook.getSheet(preferredSheetName);
            if (preferred != null) {
                return preferred;
            }

            String trimmedPreferred = preferredSheetName.trim();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getSheetName().trim().equalsIgnoreCase(trimmedPreferred)) {
                    return sheet;
                }
            }
        }

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            if (headerProbe.hasExpectedHeaders(sheet)) {
                return sheet;
            }
        }

        String label = preferredSheetName != null && !preferredSheetName.isBlank()
                ? preferredSheetName
                : "sheet with expected headers";
        throw new IllegalArgumentException("Sheet not found: " + label);
    }

    @FunctionalInterface
    interface SheetHeaderProbe {
        boolean hasExpectedHeaders(Sheet sheet);
    }
}
