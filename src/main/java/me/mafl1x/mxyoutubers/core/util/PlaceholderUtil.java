package me.mafl1x.mxyoutubers.core.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PlaceholderUtil {

    private PlaceholderUtil() {
    }

    public static String apply(String input, Map<String, String> placeholders) {
        if (input == null) {
            return "";
        }
        String result = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                result = result.replace("%" + entry.getKey() + "%", entry.getValue() == null ? "" : entry.getValue());
            }
        }
        return ColorUtil.colorize(result);
    }

    public static List<String> apply(List<String> input, Map<String, String> placeholders) {
        java.util.ArrayList<String> result = new java.util.ArrayList<String>();
        if (input == null) {
            return result;
        }
        for (String line : input) {
            result.add(apply(line, placeholders));
        }
        return result;
    }

    public static Map<String, String> map(String... values) {
        Map<String, String> placeholders = new HashMap<String, String>();
        for (int index = 0; index + 1 < values.length; index += 2) {
            placeholders.put(values[index], values[index + 1]);
        }
        return placeholders;
    }
}
