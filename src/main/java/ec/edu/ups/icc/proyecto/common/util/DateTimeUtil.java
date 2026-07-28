package ec.edu.ups.icc.proyecto.common.util;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Los instantes se almacenan y transportan en UTC (TIMESTAMPTZ / ISO 8601).
 * Para mostrar al usuario o generar reportes, se convierten a la zona de negocio.
 */
public final class DateTimeUtil {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Guayaquil");

    private DateTimeUtil() {}

    public static ZonedDateTime toBusinessZone(OffsetDateTime instant) {
        if (instant == null) return null;
        return instant.atZoneSameInstant(BUSINESS_ZONE);
    }

    public static String formatBusiness(OffsetDateTime instant, String pattern) {
        if (instant == null) return "-";
        return toBusinessZone(instant).format(DateTimeFormatter.ofPattern(pattern));
    }

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneId.of("UTC"));
    }
}
