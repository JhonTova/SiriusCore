package me.sirius.core.hook;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class Hooks {

    private boolean placeholderApi;
    private Economy economy;
    private Permission permissions;

    public void resolve() {
        hasPlaceholderApi();
        economy();
        permissions();
    }

    public Economy economy() {
        if (economy == null && vault()) {
            economy = service(Economy.class);
        }
        return economy;
    }

    public Permission permissions() {
        if (permissions == null && vault()) {
            permissions = service(Permission.class);
        }
        return permissions;
    }

    private boolean vault() {
        return Bukkit.getPluginManager().isPluginEnabled("Vault");
    }

    private <T> T service(Class<T> type) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(type);
        return provider != null ? provider.getProvider() : null;
    }

    public boolean hasPlaceholderApi() {
        if (!placeholderApi) {
            placeholderApi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        }
        return placeholderApi;
    }

    public boolean hasEconomy() {
        return economy() != null;
    }

    public boolean hasPermissions() {
        return permissions() != null;
    }

    public void unregisterExpansions(ClassLoader owner) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        try {
            Expansions.unregisterOwnedBy(owner);
        } catch (LinkageError incompatible) {
            // Otra version de PlaceholderAPI sin esta API: no hay nada que limpiar.
        }
    }

    public String placeholders(Player player, String text) {
        if (text == null || !hasPlaceholderApi()) {
            return text;
        }
        return me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, text);
    }

    public double balance(OfflinePlayer player) {
        Economy vault = economy();
        return vault != null ? vault.getBalance(player) : 0D;
    }

    public boolean hasBalance(OfflinePlayer player, double amount) {
        Economy vault = economy();
        return vault != null && vault.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        Economy vault = economy();
        if (vault == null || !vault.has(player, amount)) {
            return false;
        }
        return vault.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        Economy vault = economy();
        return vault != null && vault.depositPlayer(player, amount).transactionSuccess();
    }

    public String formatMoney(double amount) {
        Economy vault = economy();
        return vault != null ? vault.format(amount) : String.format("%,.2f", amount);
    }

    public String primaryGroup(Player player) {
        Permission vault = permissions();
        if (vault == null || !vault.hasGroupSupport()) {
            return "";
        }
        String group = vault.getPrimaryGroup(player);
        return group != null ? group : "";
    }
}
