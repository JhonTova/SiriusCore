package me.sirius.core;

import me.sirius.core.brand.Banner;
import me.sirius.core.config.Config;
import me.sirius.core.gui.Holders;
import me.sirius.core.gui.Menu;
import me.sirius.core.hook.Hooks;
import me.sirius.core.license.LicenseStatus;
import me.sirius.core.scheduler.Scheduler;
import me.sirius.core.text.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public abstract class SiriusPlugin extends JavaPlugin {

    private Scheduler scheduler;
    private Config config;
    private boolean started;

    protected abstract void onStart();

    protected void onServerReady() {
    }

    protected void onStop() {
    }

    protected String productId() {
        return getName().toLowerCase();
    }

    protected boolean requiresLicense() {
        return false;
    }

    @Override
    public final void onEnable() {
        SiriusCore core = SiriusCore.get();
        if (core == null) {
            // No debería ocurrir: plugin.yml declara depend: [SiriusCore] y Bukkit
            // no nos cargaría sin él. Es una red de seguridad por si alguien edita
            // el plugin.yml a mano para saltarse la dependencia.
            fail("SiriusCore no está cargado. Este plugin no puede funcionar sin el núcleo.");
            return;
        }

        if (requiresLicense()) {
            LicenseStatus status = core.licenses().check(productId());
            if (!status.allowsStartup()) {
                core.register(Banner.Entry.failed(getName(), version(), status.reason()));
                fail(status.reason() + ". Consulta " + core.licenses().licenseFile());
                return;
            }
        }

        this.scheduler = new Scheduler(this);

        try {
            onStart();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Fallo al arrancar " + getName(), t);
            core.register(Banner.Entry.failed(getName(), version(), "error al arrancar"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.started = true;
        core.register(Banner.Entry.ok(getName(), version()));

        scheduler.syncLater(this::serverReady, 1);
    }

    private void serverReady() {
        // Entre el enable y este tick el plugin pudo quedar desactivado: por una
        // dependencia que falló, por un /reload, o por nosotros mismos.
        if (!isEnabled() || !started) {
            return;
        }
        try {
            onServerReady();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Fallo al completar el arranque de " + getName(), t);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public final void onDisable() {
        if (!started) {
            return;
        }
        try {
            onStop();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Fallo al detener " + getName(), t);
        } finally {
            closeMenus();
            SiriusCore core = SiriusCore.get();
            if (core != null && core.hooks() != null) {
                core.hooks().unregisterExpansions(getClass().getClassLoader());
            }
            if (scheduler != null) {
                scheduler.cancelAll();
            }
            started = false;
        }
    }

    // Un menu abierto sobrevive al plugin que lo creo: el jugador podria seguir
    // pulsando botones de un plugin apagado, y el menu retiene su classloader
    // hasta que lo cierre. Se cierran los que pertenecen a este plugin.
    private void closeMenus() {
        ClassLoader own = getClass().getClassLoader();
        for (Player player : getServer().getOnlinePlayers()) {
            if (Holders.of(player.getOpenInventory().getTopInventory()) instanceof Menu menu
                    && menu.getClass().getClassLoader() == own) {
                player.closeInventory();
            }
        }
    }

    private void fail(String reason) {
        getLogger().severe(reason);
        getServer().getPluginManager().disablePlugin(this);
    }

    private String version() {
        return getDescription().getVersion();
    }


    public final Config settings() {
        if (config == null) {
            config = new Config(this, "config.yml");
        }
        return config;
    }

    public final Config settings(String name) {
        return new Config(this, name);
    }

    public final Scheduler scheduler() {
        return scheduler;
    }

    public final Hooks hooks() {
        return SiriusCore.get().hooks();
    }

    protected final void listeners(Listener... listeners) {
        for (Listener listener : listeners) {
            getServer().getPluginManager().registerEvents(listener, this);
        }
    }

    protected final void command(String name, org.bukkit.command.CommandExecutor executor) {
        var command = getCommand(name);
        if (command == null) {
            getLogger().warning("El comando '" + name + "' no está declarado en plugin.yml");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof org.bukkit.command.TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    public final void send(CommandSender target, String message) {
        Text.send(target, message);
    }
}
