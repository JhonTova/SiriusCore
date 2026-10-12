package me.sirius.core.hook;

import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.List;

// PlaceholderAPI solo retira por su cuenta las expansiones que declaran
// getRequiredPlugin(), y las de la suite son internas: con persist() seguian
// registradas despues de apagar su plugin, contestando con un objeto muerto y
// reteniendo su classloader. Solo se carga si PlaceholderAPI esta activo.
final class Expansions {

    private Expansions() {
    }

    static void unregisterOwnedBy(ClassLoader owner) {
        List<PlaceholderExpansion> owned = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager()
                .getExpansions().stream()
                .filter(expansion -> expansion.getClass().getClassLoader() == owner)
                .toList();
        for (PlaceholderExpansion expansion : owned) {
            expansion.unregister();
        }
    }
}
