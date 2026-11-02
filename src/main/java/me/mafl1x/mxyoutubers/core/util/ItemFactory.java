package me.mafl1x.mxyoutubers.core.util;

import me.mafl1x.mxyoutubers.core.config.GuiConfig;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class ItemFactory {

    private ItemFactory() {
    }

    public static ItemStack create(GuiConfig.ItemDefinition definition, Map<String, String> placeholders) {
        ItemStack item;
        if (definition.base64 != null && !definition.base64.trim().isEmpty()) {
            item = HeadUtil.createBase64Head(definition.base64.trim());
        } else if ("PLAYER_HEAD".equalsIgnoreCase(definition.material)
                && placeholders != null
                && placeholders.containsKey("player")
                && placeholders.get("player") != null
                && !placeholders.get("player").trim().isEmpty()) {
            item = HeadUtil.createPlayerHead(placeholders.get("player"));
        } else {
            Material material = Material.matchMaterial(definition.material);
            if (material == null) {
                material = Material.BARRIER;
            }
            item = new ItemStack(material, Math.max(1, definition.amount));
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(PlaceholderUtil.apply(definition.name, placeholders));
            meta.setLore(PlaceholderUtil.apply(definition.lore, placeholders));
            if (definition.customModelData > 0) {
                try {
                    meta.setCustomModelData(Integer.valueOf(definition.customModelData));
                } catch (NoSuchMethodError ignored) {
                }
            }
            if (definition.hideFlags) {
                meta.addItemFlags(ItemFlag.values());
            }
            item.setItemMeta(meta);
        }
        if (definition.enchanted) {
            item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);
        }
        return item;
    }
}
