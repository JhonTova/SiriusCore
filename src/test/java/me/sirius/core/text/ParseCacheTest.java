package me.sirius.core.text;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ParseCacheTest {

    @Test
    void noPasaDelLimite() {
        ParseCache<Integer, String> cache = new ParseCache<>(100);
        for (int i = 0; i < 10_000; i++) {
            cache.get(i, String::valueOf);
        }
        assertEquals(100, cache.size());
    }

    @Test
    void reutilizaLoYaCalculado() {
        ParseCache<String, Object> cache = new ParseCache<>(10);
        AtomicInteger calls = new AtomicInteger();
        Object first = cache.get("a", key -> {
            calls.incrementAndGet();
            return new Object();
        });
        Object second = cache.get("a", key -> {
            calls.incrementAndGet();
            return new Object();
        });
        assertSame(first, second);
        assertEquals(1, calls.get());
    }

    @Test
    void echaAlMenosUsado() {
        ParseCache<String, String> cache = new ParseCache<>(2);
        cache.get("a", k -> "A");
        cache.get("b", k -> "B");
        cache.get("a", k -> "otra");
        cache.get("c", k -> "C");

        AtomicInteger recalculated = new AtomicInteger();
        cache.get("a", k -> {
            recalculated.incrementAndGet();
            return "A2";
        });
        cache.get("b", k -> {
            recalculated.incrementAndGet();
            return "B2";
        });
        // "a" se uso el ultimo antes de meter "c": sobrevive. "b" no.
        assertEquals(1, recalculated.get());
    }
}
