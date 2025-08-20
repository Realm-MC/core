package br.com.realmmc.core.utils;

import java.util.concurrent.TimeUnit;

public final class TimeFormatter {

    private TimeFormatter() {}

    public static long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.equalsIgnoreCase("permanente")) {
            return 0; // 0 representa permanente
        }
        try {
            char unit = durationStr.toLowerCase().charAt(durationStr.length() - 1);
            long value = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));

            return switch (unit) {
                case 's' -> TimeUnit.SECONDS.toMillis(value);
                case 'm' -> TimeUnit.MINUTES.toMillis(value);
                case 'h' -> TimeUnit.HOURS.toMillis(value);
                case 'd' -> TimeUnit.DAYS.toMillis(value);
                default -> -1;
            };
        } catch (Exception e) {
            return -1;
        }
    }
}