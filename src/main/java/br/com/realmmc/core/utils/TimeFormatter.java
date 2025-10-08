package br.com.realmmc.core.utils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TimeFormatter {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwy])");

    private TimeFormatter() {}

    public static long parseDuration(String durationStr) {
        if (durationStr == null || durationStr.isBlank()) {
            return 0;
        }
        if (durationStr.equalsIgnoreCase("permanente") || durationStr.equalsIgnoreCase("eterno") || durationStr.equals("-1")) {
            return -1L;
        }

        Matcher matcher = DURATION_PATTERN.matcher(durationStr.toLowerCase());
        long totalMillis = 0;
        boolean matched = false;

        while (matcher.find()) {
            matched = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s" -> totalMillis += TimeUnit.SECONDS.toMillis(value);
                case "m" -> totalMillis += TimeUnit.MINUTES.toMillis(value);
                case "h" -> totalMillis += TimeUnit.HOURS.toMillis(value);
                case "d" -> totalMillis += TimeUnit.DAYS.toMillis(value);
                case "w" -> totalMillis += TimeUnit.DAYS.toMillis(value * 7);
                case "y" -> totalMillis += TimeUnit.DAYS.toMillis(value * 365);
            }
        }
        return matched ? totalMillis : 0;
    }

    public static String formatDuration(long millis) {
        if (millis < 0) {
            return "Permanente";
        }
        if (millis < 1000) {
            return "0s";
        }
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }

    public static String formatDetailedDuration(long millis) {
        if (millis < 0) {
            return "Permanente";
        }
        if (millis > 0 && millis < 1000) {
            millis = 1000;
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        List<String> parts = new ArrayList<>();
        if (days > 0) parts.add(days + (days == 1 ? " dia" : " dias"));
        if (hours > 0) parts.add(hours + (hours == 1 ? " hora" : " horas"));
        if (minutes > 0) parts.add(minutes + (minutes == 1 ? " minuto" : " minutos"));
        if (seconds > 0) parts.add(seconds + (seconds == 1 ? " segundo" : " segundos"));

        if (parts.isEmpty()) {
            return "agora";
        }

        if (parts.size() == 1) {
            return parts.get(0);
        }

        String lastPart = parts.remove(parts.size() - 1);
        return String.join(", ", parts) + " e " + lastPart;
    }

    public static String formatToYml(long millis) {
        if (millis <= 0) return "0s";
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        if (days > 0 && TimeUnit.DAYS.toMillis(days) == millis) return days + "d";
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        if (hours > 0 && TimeUnit.HOURS.toMillis(hours) == millis) return hours + "h";
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        if (minutes > 0 && TimeUnit.MINUTES.toMillis(minutes) == millis) return minutes + "m";
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        return seconds + "s";
    }
}