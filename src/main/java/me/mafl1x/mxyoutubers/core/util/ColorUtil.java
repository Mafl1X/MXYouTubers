package me.mafl1x.mxyoutubers.core.util;

import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String colorize(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    public static List<String> colorize(List<String> lines) {
        List<String> result = new ArrayList<String>();
        if (lines == null) {
            return result;
        }
        for (String line : lines) {
            result.add(colorize(line));
        }
        return result;
    }
}
