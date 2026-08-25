package me.sirius.core;

import me.sirius.core.brand.Banner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;

public final class SiriusCommand implements TabExecutor {

    private final SiriusCore core;

    public SiriusCommand(SiriusCore core) {
        this.core = core;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Banner.status(
                sender,
                core.getDescription().getVersion(),
                Edition.current(),
                core.licenses().status(),
                core.licenses().license(),
                core.registry());

        if (core.updates() != null && core.updates().hasUpdates()) {
            core.updates().announce(sender);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }
}
