package me.sirius.core.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;

public final class Config {

    private final Plugin plugin;
    private final String name;
    private final File file;

    private YamlConfiguration config;

    public Config(Plugin plugin, String name) {
        this.plugin = plugin;
        this.name = name.endsWith(".yml") ? name : name + ".yml";
        this.file = new File(plugin.getDataFolder(), this.name);
        reload();
    }

    public void reload() {
        if (!file.exists()) {
            plugin.saveResource(name, false);
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        mergeDefaults();
    }

    private void mergeDefaults() {
        try (InputStream stream = plugin.getResource(name)) {
            if (stream == null) {
                return;
            }
            YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(stream, StandardCharsets.UTF_8));

            boolean changed = false;
            for (String key : defaults.getKeys(true)) {
                if (!config.contains(key) && !defaults.isConfigurationSection(key)) {
                    config.set(key, defaults.get(key));
                    changed = true;
                }
            }
            if (changed) {
                save();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING,
                    "No se pudieron fusionar los valores por defecto de " + name, e);
        }
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar " + name, e);
        }
    }

    public YamlConfiguration raw() {
        return config;
    }

    public String string(String path, String fallback) {
        return config.getString(path, fallback);
    }

    public String string(String path) {
        return config.getString(path, "");
    }

    public int integer(String path, int fallback) {
        return config.getInt(path, fallback);
    }

    public double decimal(String path, double fallback) {
        return config.getDouble(path, fallback);
    }

    public boolean bool(String path, boolean fallback) {
        return config.getBoolean(path, fallback);
    }

    public List<String> strings(String path) {
        return config.getStringList(path);
    }

    public ConfigurationSection section(String path) {
        return config.getConfigurationSection(path);
    }

    public boolean has(String path) {
        return config.contains(path);
    }

    public void set(String path, Object value) {
        config.set(path, value);
    }
}
