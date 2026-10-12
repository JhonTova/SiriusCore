package me.sirius.core.text;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

// LRU acotada para el resultado de parsear MiniMessage. El parseo se hace fuera
// del candado: dos hilos pueden calcular la misma entrada a la vez, pero el
// resultado es identico y nadie espera a nadie.
final class ParseCache<K, V> {

    private final LinkedHashMap<K, V> entries;

    ParseCache(int limit) {
        this.entries = new LinkedHashMap<>(limit * 4 / 3 + 1, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    V get(K key, Function<K, V> loader) {
        synchronized (entries) {
            V cached = entries.get(key);
            if (cached != null) {
                return cached;
            }
        }
        V value = loader.apply(key);
        synchronized (entries) {
            entries.put(key, value);
        }
        return value;
    }

    int size() {
        synchronized (entries) {
            return entries.size();
        }
    }
}
