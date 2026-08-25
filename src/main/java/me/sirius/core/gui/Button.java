package me.sirius.core.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class Button {

    private final ItemStack icon;
    private final Consumer<InventoryClickEvent> action;
    private final boolean allowPickup;

    private Button(ItemStack icon, Consumer<InventoryClickEvent> action, boolean allowPickup) {
        this.icon = icon;
        this.action = action;
        this.allowPickup = allowPickup;
    }

    public static Button display(ItemStack icon) {
        return new Button(icon, event -> {}, false);
    }

    public static Button of(ItemStack icon, Consumer<Player> action) {
        return new Button(icon, event -> action.accept((Player) event.getWhoClicked()), false);
    }

    public static Button raw(ItemStack icon, Consumer<InventoryClickEvent> action) {
        return new Button(icon, action, false);
    }

    public static Button on(ItemStack icon, ClickType type, Consumer<Player> action) {
        return new Button(icon, event -> {
            if (event.getClick() == type) {
                action.accept((Player) event.getWhoClicked());
            }
        }, false);
    }

    public static Button takeable(ItemStack icon) {
        return new Button(icon, event -> {}, true);
    }

    ItemStack icon() {
        return icon;
    }

    boolean allowsPickup() {
        return allowPickup;
    }

    void onClick(InventoryClickEvent event) {
        action.accept(event);
    }
}
