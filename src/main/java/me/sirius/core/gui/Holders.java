package me.sirius.core.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

// getHolder() sobre el inventario de un bloque (cofre, tolva, horno) construye
// una copia del estado del bloque en cada llamada. Los listeners de inventario
// solo quieren saber si es un menu propio, y lo preguntan en cada clic de todo
// el servidor. Paper tiene getHolder(false), que devuelve el holder sin copia;
// se busca por reflexion porque el core compila contra la API de Spigot.
public final class Holders {

    private static final MethodHandle WITHOUT_SNAPSHOT = find();

    private Holders() {
    }

    private static MethodHandle find() {
        try {
            return MethodHandles.publicLookup().findVirtual(Inventory.class, "getHolder",
                    MethodType.methodType(InventoryHolder.class, boolean.class));
        } catch (ReflectiveOperationException spigot) {
            return null;
        }
    }

    public static InventoryHolder of(Inventory inventory) {
        if (inventory == null) {
            return null;
        }
        if (WITHOUT_SNAPSHOT != null) {
            try {
                return (InventoryHolder) WITHOUT_SNAPSHOT.invokeExact(inventory, false);
            } catch (Throwable ignored) {
                // Cae a la llamada normal.
            }
        }
        return inventory.getHolder();
    }
}
