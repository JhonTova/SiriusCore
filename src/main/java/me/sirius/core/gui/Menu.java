package me.sirius.core.gui;

import me.sirius.core.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public abstract class Menu implements InventoryHolder {

    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Inventory inventory;
    private final int size;

    protected Menu(String title, int rows) {
        this.size = Math.clamp(rows, 1, 6) * 9;
        // Los títulos de inventario nunca soportaron hover ni click, así que pasar por
        // legacy no pierde nada: los colores, incluido el hex, viajan igual.
        this.inventory = Bukkit.createInventory(this, size, Text.legacy(title));
    }

    protected abstract void build(Player viewer);

    protected void set(int slot, Button button) {
        if (slot < 0 || slot >= size) {
            return;
        }
        buttons.put(slot, button);
        inventory.setItem(slot, button.icon());
    }

    protected void set(int slot, ItemStack icon) {
        set(slot, Button.display(icon));
    }

    protected void fill(ItemStack filler) {
        for (int slot = 0; slot < size; slot++) {
            if (inventory.getItem(slot) == null) {
                inventory.setItem(slot, filler);
            }
        }
    }

    protected void border(ItemStack icon) {
        int rows = size / 9;
        for (int slot = 0; slot < size; slot++) {
            int row = slot / 9;
            int column = slot % 9;
            if (row == 0 || row == rows - 1 || column == 0 || column == 8) {
                set(slot, icon);
            }
        }
    }

    protected void clear() {
        buttons.clear();
        inventory.clear();
    }

    public void open(Player player) {
        clear();
        build(player);
        player.openInventory(inventory);
    }

    public void refresh() {
        for (var viewer : inventory.getViewers()) {
            if (viewer instanceof Player player) {
                clear();
                build(player);
                player.updateInventory();
                return;
            }
        }
    }

    void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        Button button = buttons.get(event.getRawSlot());
        if (button == null) {
            return;
        }
        if (button.allowsPickup()) {
            event.setCancelled(false);
        }
        button.onClick(event);
    }

    public void onClose(InventoryCloseEvent event) {
    }

    protected int size() {
        return size;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
