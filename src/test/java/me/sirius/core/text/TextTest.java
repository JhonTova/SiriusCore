package me.sirius.core.text;

import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TextTest {

    @Test
    void laCacheDaLoMismoQueParsearDeCero() {
        String line = "&7Proxima recompensa en &f<remaining>&7.";
        Map<String, String> data = Map.of("remaining", "5m 3s");
        String first = Text.legacy(line, data);
        String second = Text.legacy(line, data);
        assertEquals(first, second);
        assertEquals("§7Proxima recompensa en §f5m 3s§7.", first);
    }

    @Test
    void unMarcadorQueNoApareceNoCambiaElResultado() {
        String line = "&7Cada panel son &f<each>&7.";
        assertEquals(
                Text.legacy(line, Map.of("each", "1m")),
                Text.legacy(line, Map.of("each", "1m", "remaining", "otro valor cada segundo")));
    }

    @Test
    void sinMarcadoresUsadosReutilizaElComponente() {
        Component a = Text.of("&aHola", Map.of("x", "1"));
        Component b = Text.of("&aHola", Map.of("x", "2"));
        assertSame(a, b);
    }

    @Test
    void losValoresNoSeInterpretanComoEtiquetas() {
        String out = Text.plain(Text.of("Hola <name>", Map.of("name", "<red>pirata</red>")));
        assertEquals("Hola <red>pirata</red>", out);
    }

    @Test
    void elMarcadorSeReconoceSinImportarMayusculas() {
        assertEquals("Faltan 4s", Text.plain(Text.of("Faltan <REMAINING>", Map.of("remaining", "4s"))));
    }

    @Test
    void lasDosCachesCoinciden() {
        String line = "&#ff0000rojo <green>verde</green> &lnegrita <n>";
        Map<String, String> data = Map.of("n", "7");
        assertEquals(Text.legacy(Text.of(line, data)), Text.legacy(line, data));
        assertEquals(Text.legacy(Text.of("&#ff0000rojo")), Text.legacy("&#ff0000rojo"));
    }

    @Test
    void textoVacioONulo() {
        assertEquals("", Text.legacy(""));
        assertEquals("", Text.legacy(null, Map.of()));
        assertEquals(Component.empty(), Text.of((String) null));
    }
}
