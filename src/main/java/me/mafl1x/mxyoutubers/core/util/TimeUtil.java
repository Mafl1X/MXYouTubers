package me.mafl1x.mxyoutubers.core.util;

import java.time.Duration;

public final class TimeUtil {

    private TimeUtil() {
    }

    public static String formatDuration(Duration duration) {
        long seconds = Math.max(0L, duration.getSeconds());
        long days = seconds / 86400L;
        long hours = (seconds % 86400L) / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        if (days > 0L) {
            return days + "д " + hours + "ч";
        }
        if (hours > 0L) {
            return hours + "ч " + minutes + "м";
        }
        return Math.max(1L, minutes) + "м";
    }
}
