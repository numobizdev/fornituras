package mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.uumbal.solutions.palm_flow.modules.auth.service.EmailService;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.CalidadFruta;
import mx.uumbal.solutions.palm_flow.modules.recepcion_fruta.entity.RecepcionFruta;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecepcionFrutaEmailService {

    private static final DateTimeFormatter FECHA_FORMAT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", new Locale("es", "MX"));
    private static final Locale MX_LOCALE = new Locale("es", "MX");

    private final EmailService emailService;

    public void notifyProductorIfEmailPresent(RecepcionFruta recepcion) {
        String correo = recepcion.getProductor().getCorreoElectronico();
        if (!StringUtils.hasText(correo)) {
            log.debug(
                    "Productor {} sin correo registrado; se omite notificación de recepción {}",
                    recepcion.getProductor().getNombre(),
                    recepcion.getFolio());
            return;
        }

        String to = correo.trim();
        String subject = "Recepción de fruta " + recepcion.getFolio();
        emailService.sendHtmlEmail(to, subject, "email/recepcion-fruta", buildTemplateVariables(recepcion));
        log.info("Notificación de recepción {} enviada a productor {}", recepcion.getFolio(), to);
    }

    private Map<String, Object> buildTemplateVariables(RecepcionFruta recepcion) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("productorNombre", recepcion.getProductor().getNombre());
        variables.put("folio", recepcion.getFolio());
        variables.put("fecha", formatFecha(recepcion.getFecha()));
        variables.put("centroAcopioNombre", recepcion.getCentroAcopio().getNombre());
        variables.put("usuarioEmail", recepcion.getUsuario().getEmail());
        variables.put("predioNombre", recepcion.getPredio().getNombre());
        variables.put(
                "loteNombre",
                recepcion.getLote() != null ? recepcion.getLote().getNombre() : "Lote desconocido");
        variables.put(
                "calidadFrutaLabel",
                CalidadFruta.formatLabels(recepcion.getCalidadFruta()));
        variables.put("pesoBruto", formatKg(recepcion.getPesoBruto()));
        variables.put("pesoTara", formatKg(recepcion.getPesoTara()));
        variables.put("pesoNeto", formatKg(recepcion.getPesoNeto()));
        variables.put(
                "origenPeso",
                recepcion.getOrigenPeso() != null ? recepcion.getOrigenPeso().name() : null);
        variables.put("precioKg", formatCurrency(recepcion.getPrecioKg()));
        variables.put(
                "precioKgConUnidad",
                recepcion.getPrecioKg() != null ? formatCurrency(recepcion.getPrecioKg()) + " / kg" : null);
        variables.put("montoAPagar", formatCurrency(recepcion.getMontoAPagar()));
        variables.put("placa", recepcion.getPlaca());
        variables.put("marca", recepcion.getMarca());
        variables.put("modelo", recepcion.getModelo());
        variables.put("hasUsuarioEmail", StringUtils.hasText(recepcion.getUsuario().getEmail()));
        variables.put("hasPesoBruto", recepcion.getPesoBruto() != null);
        variables.put("hasPesoTara", recepcion.getPesoTara() != null);
        variables.put("hasPesoNeto", recepcion.getPesoNeto() != null);
        variables.put("hasOrigenPeso", recepcion.getOrigenPeso() != null);
        variables.put(
                "hasCalidadFruta",
                recepcion.getCalidadFruta() != null && !recepcion.getCalidadFruta().isEmpty());
        variables.put("hasPayment", recepcion.getPrecioKg() != null || recepcion.getMontoAPagar() != null);
        variables.put(
                "hasTransport",
                StringUtils.hasText(recepcion.getPlaca())
                        || StringUtils.hasText(recepcion.getMarca())
                        || StringUtils.hasText(recepcion.getModelo()));
        return variables;
    }

    private String formatFecha(Instant fecha) {
        if (fecha == null) {
            return "";
        }
        return FECHA_FORMAT.format(fecha.atZone(ZoneId.of("America/Mexico_City")));
    }

    private String formatKg(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.stripTrailingZeros().toPlainString() + " kg";
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return null;
        }
        NumberFormat formatter = NumberFormat.getCurrencyInstance(MX_LOCALE);
        return formatter.format(value);
    }
}
