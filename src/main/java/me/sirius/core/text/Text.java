package me.sirius.core.text;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private static final LegacyComponentSerializer SECTION_HEX = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static BukkitAudiences audiences;

    private Text() {
    }

    public static void init(Plugin plugin) {
        if (audiences == null) {
            audiences = BukkitAudiences.create(plugin);
        }
    }

    public static void close() {
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
    }

    // Los menus se repintan cada segundo con los mismos textos: parsear MiniMessage
    // cada vez era lo que mas pesaba de toda la suite en el hilo principal.
    private static final ParseCache<Object, Component> COMPONENTS = new ParseCache<>(4096);
    private static final ParseCache<Object, String> LEGACY = new ParseCache<>(4096);

    private record Template(String input, Map<String, String> values) {
    }

    public static Component of(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return COMPONENTS.get(input, key -> parse(input, Map.of()));
    }

    public static Component of(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        Map<String, String> used = used(input, placeholders);
        if (used.isEmpty()) {
            return of(input);
        }
        return COMPONENTS.get(new Template(input, used), key -> parse(input, used));
    }

    private static Component parse(String input, Map<String, String> placeholders) {
        if (placeholders.isEmpty()) {
            return MINI.deserialize(legacyToTags(input)).compact();
        }
        List<TagResolver> resolvers = new ArrayList<>(placeholders.size());
        placeholders.forEach((key, value) -> resolvers.add(Placeholder.unparsed(key, value)));

        return MINI.deserialize(legacyToTags(input), TagResolver.resolver(resolvers)).compact();
    }

    // Solo los marcadores que aparecen en el texto entran en la clave de la cache.
    // Asi una linea sin marcadores se reutiliza aunque el mapa traiga un contador
    // que cambia cada segundo.
    private static Map<String, String> used(String input, Map<String, String> placeholders) {
        if (placeholders.isEmpty() || input.indexOf('<') < 0) {
            return Map.of();
        }
        Map<String, String> used = null;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (mentions(input, entry.getKey())) {
                if (used == null) {
                    used = new HashMap<>();
                }
                used.put(entry.getKey(), entry.getValue());
            }
        }
        return used == null ? Map.of() : used;
    }

    private static boolean mentions(String input, String key) {
        int last = input.length() - key.length() - 1;
        for (int at = input.indexOf('<'); at >= 0 && at <= last; at = input.indexOf('<', at + 1)) {
            if (input.regionMatches(true, at + 1, key, 0, key.length())) {
                return true;
            }
        }
        return false;
    }

    public static List<Component> of(List<String> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(of(line));
        }
        return out;
    }

    public static String plain(Component component) {
        return PLAIN.serialize(component);
    }

    public static String strip(String input) {
        return plain(of(input));
    }

    public static String legacy(Component component) {
        return SECTION_HEX.serialize(component);
    }

    public static String legacy(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return LEGACY.get(input, key -> legacy(parse(input, Map.of())));
    }

    public static String legacy(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        Map<String, String> used = used(input, placeholders);
        if (used.isEmpty()) {
            return legacy(input);
        }
        return LEGACY.get(new Template(input, used), key -> legacy(parse(input, used)));
    }

    public static void send(CommandSender target, String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        audience(target).sendMessage(of(message));
    }

    public static void send(CommandSender target, String message, Map<String, String> placeholders) {
        if (message == null || message.isBlank()) {
            return;
        }
        audience(target).sendMessage(of(message, placeholders));
    }

    public static void sendClickable(CommandSender target, String message,
                                     String clickableText, String runCommand, String hoverText) {
        Component clickable = of(clickableText)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(runCommand))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(of(hoverText)));
        audience(target).sendMessage(of(message).append(Component.space()).append(clickable));
    }

    public static void send(CommandSender target, Component message) {
        audience(target).sendMessage(message);
    }

    public static void actionBar(Player player, String message) {
        audiences.player(player).sendActionBar(of(message));
    }

    public static void title(Player player, String title, String subtitle,
                             int fadeIn, int stay, int fadeOut) {
        audiences.player(player).showTitle(net.kyori.adventure.title.Title.title(
                of(title),
                of(subtitle),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(fadeIn * 50L),
                        java.time.Duration.ofMillis(stay * 50L),
                        java.time.Duration.ofMillis(fadeOut * 50L))));
    }

    public static Audience audience(CommandSender sender) {
        return audiences.sender(sender);
    }

    public static void broadcast(String message) {
        audiences.all().sendMessage(of(message));
    }

    private static final Map<Character, String> LEGACY_TAGS = Map.ofEntries(
            Map.entry('0', "<black>"), Map.entry('1', "<dark_blue>"),
            Map.entry('2', "<dark_green>"), Map.entry('3', "<dark_aqua>"),
            Map.entry('4', "<dark_red>"), Map.entry('5', "<dark_purple>"),
            Map.entry('6', "<gold>"), Map.entry('7', "<gray>"),
            Map.entry('8', "<dark_gray>"), Map.entry('9', "<blue>"),
            Map.entry('a', "<green>"), Map.entry('b', "<aqua>"),
            Map.entry('c', "<red>"), Map.entry('d', "<light_purple>"),
            Map.entry('e', "<yellow>"), Map.entry('f', "<white>"),
            Map.entry('k', "<obfuscated>"), Map.entry('l', "<bold>"),
            Map.entry('m', "<strikethrough>"), Map.entry('n', "<underlined>"),
            Map.entry('o', "<italic>"), Map.entry('r', "<reset>"));

    private static final java.util.regex.Pattern LEGACY_HEX =
            java.util.regex.Pattern.compile("[&§]#([0-9A-Fa-f]{6})");
    private static final java.util.regex.Pattern LEGACY_CODE =
            java.util.regex.Pattern.compile("[&§]([0-9A-Fa-fk-orK-OR])");

    private static String legacyToTags(String input) {
        if (input.indexOf('&') < 0 && input.indexOf('§') < 0) {
            return input;
        }

        java.util.regex.Matcher hex = LEGACY_HEX.matcher(input);
        StringBuilder afterHex = new StringBuilder();
        while (hex.find()) {
            hex.appendReplacement(afterHex, "<#" + hex.group(1) + ">");
        }
        hex.appendTail(afterHex);

        java.util.regex.Matcher code = LEGACY_CODE.matcher(afterHex);
        StringBuilder out = new StringBuilder();
        while (code.find()) {
            String tag = LEGACY_TAGS.get(Character.toLowerCase(code.group(1).charAt(0)));
            code.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(tag));
        }
        code.appendTail(out);
        return out.toString();
    }
}
