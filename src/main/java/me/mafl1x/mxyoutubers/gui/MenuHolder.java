package me.mafl1x.mxyoutubers.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MenuHolder implements InventoryHolder {

    private final GuiType type;
    private final int page;
    private final Long applicationId;
    private final UUID targetPlayerUuid;
    private final Map<Integer, Long> applicationSlots = new HashMap<Integer, Long>();
    private final Map<Integer, UUID> targetSlots = new HashMap<Integer, UUID>();
    private Inventory inventory;

    public MenuHolder(GuiType type, int page, Long applicationId, UUID targetPlayerUuid) {
        this.type = type;
        this.page = page;
        this.applicationId = applicationId;
        this.targetPlayerUuid = targetPlayerUuid;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public GuiType getType() {
        return type;
    }

    public int getPage() {
        return page;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public UUID getTargetPlayerUuid() {
        return targetPlayerUuid;
    }

    public Map<Integer, Long> getApplicationSlots() {
        return applicationSlots;
    }

    public Map<Integer, UUID> getTargetSlots() {
        return targetSlots;
    }
}
