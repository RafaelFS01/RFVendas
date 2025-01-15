package BackEnd.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static String formatarData(LocalDate data) {
        if (data == null) return "";
        return data.format(DATE_FORMATTER);
    }

    public static String formatarDataHora(LocalDateTime dataHora) {
        if (dataHora == null) return "";
        return dataHora.format(DATE_TIME_FORMATTER);
    }

    public static LocalDate parseData(String data) {
        if (data == null || data.trim().isEmpty()) return null;
        return LocalDate.parse(data, DATE_FORMATTER);
    }

    public static LocalDateTime parseDataHora(String dataHora) {
        if (dataHora == null || dataHora.trim().isEmpty()) return null;
        return LocalDateTime.parse(dataHora, DATE_TIME_FORMATTER);
    }
}