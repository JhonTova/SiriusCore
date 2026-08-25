package me.sirius.core;

import me.sirius.core.brand.Banner;
import me.sirius.core.config.Config;
import me.sirius.core.gui.MenuListener;
import me.sirius.core.hook.Hooks;
import me.sirius.core.license.LicenseManager;
import me.sirius.core.scheduler.Scheduler;
import me.sirius.core.text.Text;
import me.sirius.core.update.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SiriusCore extends JavaPlugin {

    private static SiriusCore instance;

    private final List<Banner.Entry> registry = Collections.synchronizedList(new ArrayList<>());

    private static final String DEFAULT_MANIFEST =
            "https://raw.githubusercontent.com/JhonTova/SiriusCore/main/updates.json";

    private LicenseManager licenses;
    private UpdateChecker updates;
    private Hooks hooks;
    private Scheduler scheduler;
    private long startedAt;

    public static SiriusCore get() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.startedAt = System.currentTimeMillis();

        Text.init(this);
        this.scheduler = new Scheduler(this);

        Config config = new Config(this, "config.yml");

        // La licencia se carga siempre; cada plugin decide si la exige con requiresLicense().
        // No hay interruptor para desactivar la verificación: la única forma de autorizar un
        // plugin premium es una licencia firmada, incluso para el servidor del propio autor.
        this.licenses = new LicenseManager(getDataFolder().toPath());
        this.licenses.load();

        this.hooks = new Hooks();

        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        var siriusCmd = getCommand("sirius");
        if (siriusCmd != null) {
            siriusCmd.setExecutor(new SiriusCommand(this));
        }

        // Las integraciones, el banner y la comprobación de actualizaciones se
        // resuelven en el primer tick: para entonces el servidor ya cargó todos los
        // plugins, así que Vault y PlaceholderAPI existen y el registro está completo.
        // Tiene que ser syncLater y no sync: sync ejecuta en el acto cuando ya estamos
        // en el hilo principal, que es justo el caso aquí, y eso lo dejaba todo
        // resuelto dentro de este mismo onEnable, antes que ningún otro plugin.
        scheduler.syncLater(() -> {
            hooks.resolve();

            Banner.startup(
                    getDescription().getVersion(),
                    Edition.current(),
                    licenses.status(),
                    licenses.license(),
                    List.copyOf(registry),
                    System.currentTimeMillis() - startedAt);

            startUpdateChecker(config);
        }, 1);
    }

    private void startUpdateChecker(Config config) {
        if (!config.bool("updates.check", true)) {
            return;
        }
        this.updates = new UpdateChecker(scheduler, getLogger(),
                config.string("updates.manifest", DEFAULT_MANIFEST));

        getServer().getPluginManager().registerEvents(
                new UpdateChecker.JoinNotifier(updates, scheduler), this);

        // El core también se anuncia a sí mismo, así que un fallo suyo también avisa.
        Map<String, String> installed = new HashMap<>();
        installed.put("siriuscore", getDescription().getVersion());
        for (Banner.Entry entry : registry) {
            installed.put(entry.name().toLowerCase(), entry.version());
        }

        updates.start(installed, config.integer("updates.interval-hours", 6));
    }

    @Override
    public void onDisable() {
        Banner.shutdown(getDescription().getVersion(), registry.size());

        if (scheduler != null) {
            scheduler.cancelAll();
        }
        registry.clear();
        Text.close();
        instance = null;
    }

    public void register(Banner.Entry entry) {
        registry.add(entry);
    }

    public LicenseManager licenses() {
        return licenses;
    }

    public Hooks hooks() {
        return hooks;
    }

    public UpdateChecker updates() {
        return updates;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    public List<Banner.Entry> registry() {
        return List.copyOf(registry);
    }
}
