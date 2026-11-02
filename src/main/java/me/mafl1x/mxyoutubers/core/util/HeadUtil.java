package me.mafl1x.mxyoutubers.core.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

public final class HeadUtil {

    private HeadUtil() {
    }

    public static ItemStack createPlayerHead(String playerName) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            meta.setOwningPlayer(player);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createBase64Head(String base64) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }
        try {
            String texture = normalizeTexture(base64);
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "mxyoutubers");
            Method getProperties = gameProfileClass.getMethod("getProperties");
            Object propertyMap = getProperties.invoke(profile);
            Method put = propertyMap.getClass().getMethod("put", Object.class, Object.class);
            Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", texture);
            put.invoke(propertyMap, "textures", property);
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
            item.setItemMeta(meta);
        } catch (Exception ignored) {
        }
        return item;
    }

    private static String normalizeTexture(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + trimmed + "\"}}}")
                    .getBytes(StandardCharsets.UTF_8));
        }
        try {
            Base64.getDecoder().decode(trimmed);
            return trimmed;
        } catch (IllegalArgumentException ignored) {
            return Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + trimmed + "\"}}}")
                    .getBytes(StandardCharsets.UTF_8));
        }
    }
}
