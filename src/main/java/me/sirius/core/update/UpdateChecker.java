package me.sirius.core.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.sirius.core.scheduler.Scheduler;
import me.sirius.core.text.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class UpdateChecker {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final Map<String, Update> available = new ConcurrentHashMap<>();
    private final Scheduler scheduler;
    private final java.util.logging.Logger logger;
    private final String manifestUrl;

    public UpdateChecker(Scheduler scheduler, java.util.logging.Logger logger, String manifestUrl) {
        this.scheduler = scheduler;
        this.logger = logger;
        this.manifestUrl = manifestUrl;
    }

    public void start(Map<String, String> installedVersions, long intervalHours) {
        check(installedVersions);

        if (intervalHours > 0) {
            long ticks = intervalHours * 60 * 60 * 20;
            scheduler.asyncTimer(() -> check(installedVersions), ticks, ticks);
        }
    }

    private void check(Map<String, String> installedVersions) {
        scheduler.async(() -> {
            try {
                JsonObject manifest = fetch();
                Map<String, Update> found = new HashMap<>();

                installedVersions.forEach((product, installed) -> {
                    JsonElement entry = manifest.get(product);
                    if (entry == null || !entry.isJsonObject()) {
                        return;
                    }
                    JsonObject data = entry.getAsJsonObject();
                    String latest = string(data, "version", "");

                    if (!latest.isEmpty() && isNewer(latest, installed)) {
                        found.put(product, new Update(
                                product,
                                installed,
                                latest,
                                string(data, "url", "https://github.com/JhonTova"),
                                Severity.parse(string(data, "severity", "normal")),
                                string(data, "message", "")));
                    }
                });

                available.clear();
                available.putAll(found);

                if (!found.isEmpty()) {
                    scheduler.sync(() -> announce(Bukkit.getConsoleSender()));
                }
            } catch (Exception e) {
                // Sin red, sin GitHub, o manifiesto mal formado: no es asunto del
                // administrador. Solo queda constancia en modo depuración.
                logger.log(Level.FINE, "No se pudo comprobar actualizaciones", e);
            }
        });
    }

    private JsonObject fetch() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(manifestUrl))
                .header("User-Agent", "SiriusCore")
                .timeout(TIMEOUT)
                .GET()
                .build();

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("HTTP " + response.statusCode());
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        }
    }

    public void announce(CommandSender target) {
        if (available.isEmpty()) {
            return;
        }
        Text.send(target, "");
        Text.send(target, "  <gradient:#00d4ff:#7b2ff7><bold>SIRIUS</bold></gradient> <gray>· actualizaciones disponibles");

        for (Update update : available.values()) {
            Text.send(target, "  " + update.render());
            if (!update.message().isEmpty()) {
                Text.send(target, "      <gray><italic>" + update.message());
            }
        }
        Text.send(target, "  <dark_gray>github.com/JhonTova");
        Text.send(target, "");
    }

    public List<Update> available() {
        return List.copyOf(available.values());
    }

    public boolean hasUpdates() {
        return !available.isEmpty();
    }

    public boolean hasUrgent() {
        return available.values().stream().anyMatch(u -> u.severity() == Severity.URGENT);
    }

    static boolean isNewer(String candidate, String current) {
        int[] a = parse(candidate);
        int[] b = parse(current);

        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int left = i < a.length ? a[i] : 0;
            int right = i < b.length ? b[i] : 0;
            if (left != right) {
                return left > right;
            }
        }
        return false;
    }

    private static int[] parse(String version) {
        String[] parts = version.replaceAll("[^0-9.]", "").split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                numbers[i] = parts[i].isEmpty() ? 0 : Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    public enum Severity {
        NORMAL, RECOMMENDED, URGENT;

        static Severity parse(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return NORMAL;
            }
        }

        String color() {
            return switch (this) {
                case URGENT -> "<red>";
                case RECOMMENDED -> "<yellow>";
                case NORMAL -> "<green>";
            };
        }

        String label() {
            return switch (this) {
                case URGENT -> "URGENTE";
                case RECOMMENDED -> "recomendada";
                case NORMAL -> "disponible";
            };
        }
    }

    public record Update(String product, String installed, String latest,
                         String url, Severity severity, String message) {

        String render() {
            return severity.color() + "● <white>" + product
                    + " <dark_gray>" + installed + " <gray>→ " + severity.color() + latest
                    + "  <dark_gray>(" + severity.label() + ")";
        }
    }

    public static final class JoinNotifier implements org.bukkit.event.Listener {

        public static final String PERMISSION = "sirius.updates";

        private final UpdateChecker checker;
        private final Scheduler scheduler;

        public JoinNotifier(UpdateChecker checker, Scheduler scheduler) {
            this.checker = checker;
            this.scheduler = scheduler;
        }

        @org.bukkit.event.EventHandler
        public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
            if (!checker.hasUpdates() || !event.getPlayer().hasPermission(PERMISSION)) {
                return;
            }
            // Un par de segundos de margen: el aviso se pierde entre el spam de
            // mensajes de bienvenida si se manda en el mismo tick que el join.
            scheduler.syncLater(() -> {
                if (event.getPlayer().isOnline()) {
                    checker.announce(event.getPlayer());
                }
            }, 40L);
        }
    }
}
