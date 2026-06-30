package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity;

import java.util.List;
import java.util.stream.Collectors;

public enum CalidadFruta {
    RACIMO_VERDE("Racimo verde"),
    RACIMO_MADURO("Racimo maduro"),
    RACIMO_SOBRE_MADURO("Racimo sobre maduro"),
    RACIMO_PODRIDO("Racimo podrido"),
    PEDUNCULO_PINZOTE_LARGO("Pedúnculo y/o pinzote largo");

    private final String label;

    CalidadFruta(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static String formatLabels(List<CalidadFruta> calidades) {
        if (calidades == null || calidades.isEmpty()) {
            return "";
        }
        return calidades.stream().map(CalidadFruta::getLabel).collect(Collectors.joining(", "));
    }
}
