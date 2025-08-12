// PASTA: core/src/main/java/br/com/realmmc/core/utils/
package br.com.realmmc.core.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class TimeDifferenceFormatter {

    private TimeDifferenceFormatter() {}

    /**
     * Formata a duração entre duas datas em uma string legível (dias, horas, minutos).
     * @param start A data de início.
     * @param end   A data final.
     * @return Uma string formatada, ex: "3 dias, 5 horas, 10 minutos".
     */
    public static String format(Date start, Date end) {
        if (start == null || end == null) {
            return "Indisponível";
        }

        long millis = end.getTime() - start.getTime();

        if (millis < 60000) { // Menos de 1 minuto
            return "Menos de um minuto";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(days == 1 ? " dia" : " dias").append(", ");
        if (hours > 0) sb.append(hours).append(hours == 1 ? " hora" : " horas").append(", ");
        if (minutes > 0) sb.append(minutes).append(minutes == 1 ? " minuto" : " minutos");

        String result = sb.toString();
        // Remove a vírgula final, se houver
        if (result.endsWith(", ")) {
            result = result.substring(0, result.length() - 2);
        }

        return result;
    }
}