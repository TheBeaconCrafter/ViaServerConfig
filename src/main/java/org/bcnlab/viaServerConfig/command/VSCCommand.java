package org.bcnlab.viaServerConfig.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bcnlab.viaServerConfig.ViaServerConfig;

public class VSCCommand implements SimpleCommand {
    private final ViaServerConfig plugin;

    public VSCCommand(ViaServerConfig plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        source.sendMessage(
                plugin.getPrefix()
                        .append(Component.text("BeaconLabsVelocity Version ", NamedTextColor.RED))
                        .append(Component.text(plugin.getVersion() + " ", NamedTextColor.GOLD))
                        .append(Component.text("by ItsBeacon", NamedTextColor.RED))
        );
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
