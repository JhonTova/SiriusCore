package me.sirius.core.item;

import me.sirius.core.text.Text;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    private ItemBuilder(ItemStack item) {
        this.item = item;
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(new ItemStack(material));
    }

    public static ItemBuilder of(Material material, int amount) {
        return new ItemBuilder(new ItemStack(material, amount));
    }

    public static ItemBuilder copyOf(ItemStack source) {
        return new ItemBuilder(source.clone());
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(Math.clamp(amount, 1, item.getMaxStackSize()));
        return this;
    }

    private static final String NO_ITALIC = "§r";

    public ItemBuilder name(String name) {
        meta.setDisplayName(NO_ITALIC + Text.legacy(name));
        return this;
    }

    public ItemBuilder name(String name, Map<String, String> placeholders) {
        meta.setDisplayName(NO_ITALIC + Text.legacy(Text.of(name, placeholders)));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        return lore(Arrays.asList(lines));
    }

    public ItemBuilder lore(List<String> lines) {
        List<String> lore = new ArrayList<>(lines.size());
        for (String line : lines) {
            lore.add(NO_ITALIC + Text.legacy(line));
        }
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder lore(List<String> lines, Map<String, String> placeholders) {
        List<String> lore = new ArrayList<>(lines.size());
        for (String line : lines) {
            lore.add(NO_ITALIC + Text.legacy(Text.of(line, placeholders)));
        }
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder glow() {
        meta.setEnchantmentGlintOverride(true);
        return this;
    }

    public ItemBuilder flags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder hideAll() {
        meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder unbreakable() {
        meta.setUnbreakable(true);
        return this;
    }

    public ItemBuilder model(int customModelData) {
        meta.setCustomModelData(customModelData);
        return this;
    }

    public ItemBuilder tag(Plugin plugin, String key, String value) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        return this;
    }

    public ItemBuilder tag(Plugin plugin, String key, int value) {
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, key), PersistentDataType.INTEGER, value);
        return this;
    }

    public ItemBuilder skull(UUID owner) {
        if (meta instanceof SkullMeta skull) {
            skull.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(owner));
        }
        return this;
    }

    public ItemBuilder skull(String textureUrl) {
        if (meta instanceof SkullMeta skull) {
            try {
                PlayerProfile profile = org.bukkit.Bukkit.createPlayerProfile(UUID.randomUUID(), "");
                profile.getTextures().setSkin(URI.create(textureUrl).toURL());
                skull.setOwnerProfile(profile);
            } catch (MalformedURLException | IllegalArgumentException e) {
                throw new IllegalArgumentException("URL de textura inválida: " + textureUrl, e);
            }
        }
        return this;
    }

    public ItemStack build() {
        item.setItemMeta(meta);
        return item;
    }

    public static String readTag(ItemStack item, Plugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, key), PersistentDataType.STRING);
    }

    public static boolean hasTag(ItemStack item, Plugin plugin, String key) {
        return readTag(item, plugin, key) != null;
    }
}
