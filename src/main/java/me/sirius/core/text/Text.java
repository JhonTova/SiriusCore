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

    public static Component of(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MINI.deserialize(legacyToTags(input)).compact();
    }

    public static Component of(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        List<TagResolver> resolvers = new ArrayList<>(placeholders.size());
        placeholders.forEach((key, value) -> resolvers.add(Placeholder.unparsed(key, value)));

        return MINI.deserialize(legacyToTags(input), TagResolver.resolver(resolvers)).compact();
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
        return legacy(of(input));
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
