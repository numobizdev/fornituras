package mx.uumbal.solutions.palm_flow.modules.productores.util;

import java.util.Locale;

public final class FiscalIdNormalizer {

    private FiscalIdNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.matches("\\d+\\.0+")) {
            trimmed = trimmed.replaceAll("\\.0+$", "");
        }
        return trimmed;
    }

    public static String key(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }
}
