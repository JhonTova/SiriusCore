package me.sirius.core.brand;

import me.sirius.core.license.License;
import me.sirius.core.license.LicenseStatus;
import me.sirius.core.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

public final class Banner {

    private static final String LINE = "<dark_gray>────────────────────────────────────────────────────────────";

    private Banner() {
    }

    public static void startup(String coreVersion, String edition,
                               LicenseStatus status, License license,
                               List<Entry> plugins, long millis) {
        panel(Bukkit.getConsoleSender(), coreVersion, edition, status, license, plugins,
                "<gray>listo en <white>" + millis + "ms");
    }

    public static void status(CommandSender target, String coreVersion, String edition,
                              LicenseStatus status, License license, List<Entry> plugins) {
        panel(target, coreVersion, edition, status, license, plugins, "<gray>estado en vivo");
    }

    private static void panel(CommandSender target, String coreVersion, String edition,
                              LicenseStatus status, License license, List<Entry> plugins,
                              String footer) {
        line(target, "");
        line(target, "  <gradient:#00d4ff:#7b2ff7><bold>███  S I R I U S</bold></gradient>");
        line(target, "  <gray>suite de plugins  <dark_gray>·  <gray>core <white>v" + coreVersion
                + "  <dark_gray>·  <gray>edición <white>" + edition);
        line(target, "  <dark_gray>github.com/JhonTova   ·   discord: ksirius");
        line(target, LINE);

        if (plugins.isEmpty()) {
            line(target, "  <yellow>Ningún plugin de la suite ha sido cargado todavía.");
        }
        for (Entry entry : plugins) {
            line(target, entry.render());
        }

        line(target, LINE);
        line(target, "  " + summary(status, license, plugins, footer));
        line(target, "");
    }

    public static void shutdown(String coreVersion, int pluginCount) {
        CommandSender console = Bukkit.getConsoleSender();
        line(console, "");
        line(console, LINE);
        line(console, "  <gradient:#00d4ff:#7b2ff7><bold>SIRIUS</bold></gradient>"
                + "  <gray>core <white>v" + coreVersion + " <gray>detenido"
                + "  <dark_gray>·  <gray>" + pluginCount + " plugin(s) descargado(s), datos guardados");
        line(console, LINE);
        line(console, "");
    }

    private static String summary(LicenseStatus status, License license,
                                  List<Entry> plugins, String footer) {
        long active = plugins.stream().filter(Entry::enabled).count();

        String licenseInfo = switch (status) {
            case FREE, MISSING -> "<gray>licencia: <white>no instalada";
            case VALID -> "<gray>licencia: <white>" + license.buyer()
                    + " <dark_gray>(" + license.watermark() + ")"
                    + (license.isPerpetual() ? "" : " <gray>hasta <white>" + license.expiresAt());
            default -> "<red>licencia: " + status.reason();
        };

        return "<white>" + plugins.size() + " <gray>plugin(s)  <dark_gray>·  <green>"
                + active + " <gray>activo(s)  <dark_gray>·  " + licenseInfo
                + "  <dark_gray>·  " + footer;
    }

    private static void line(CommandSender target, String message) {
        Text.send(target, message);
    }

    public record Entry(String name, String version, boolean enabled, String note) {

        public static Entry ok(String name, String version) {
            return new Entry(name, version, true, "");
        }

        public static Entry failed(String name, String version, String note) {
            return new Entry(name, version, false, note);
        }

        String render() {
            String icon = enabled ? "<green>✔" : "<red>✖";
            String padded = (name + " ".repeat(Math.max(1, 18 - name.length())));
            String tail = note.isEmpty() ? "" : "  <red>" + note;
            return "  " + icon + " <white>" + padded + "<dark_gray>v" + version + tail;
        }
    }
}
