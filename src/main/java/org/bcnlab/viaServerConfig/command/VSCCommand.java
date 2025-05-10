package org.bcnlab.viaServerConfig.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bcnlab.viaServerConfig.ViaServerConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VSCCommand implements SimpleCommand {
    private final ViaServerConfig plugin;
    private final ProxyServer server;

    public VSCCommand(ViaServerConfig plugin) {
        this.plugin = plugin;
        this.server = plugin.getServer();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length > 0) {
            // Handle reload command
            if (args[0].equalsIgnoreCase("reload")) {
                handleReload(source);
                return;
            }
            
            // Handle bypass command
            if (args[0].equalsIgnoreCase("bypass")) {
                handleBypass(source, args);
                return;
            }
        }

        // Default command info
        source.sendMessage(
                plugin.getPrefix()
                        .append(Component.text("ViaServerConfig Version ", NamedTextColor.RED))
                        .append(Component.text(plugin.getVersion() + " ", NamedTextColor.GOLD))
                        .append(Component.text("by ItsBeacon", NamedTextColor.RED))
        );
    }

    /**
     * Handle the reload command
     */
    private void handleReload(CommandSource source) {
        if (source.hasPermission("vsc.reload")) {
            plugin.reloadConfig();
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("Configuration reloaded successfully", NamedTextColor.GREEN))
            );
        } else {
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("You don't have permission to reload the config", NamedTextColor.RED))
            );
        }
    }
    
    /**
     * Handle the bypass command
     */
    private void handleBypass(CommandSource source, String[] args) {
        // Check self-bypass permission
        if (!source.hasPermission("beaconlabs.viasc.bypass")) {
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("You don't have permission to use bypass", NamedTextColor.RED))
            );
            return;
        }
        
        // If no player specified, toggle for self (if source is a player)
        if (args.length == 1) {
            if (source instanceof Player) {
                Player player = (Player) source;
                toggleBypass(source, player.getUsername(), player.getUniqueId());
            } else {
                source.sendMessage(
                        plugin.getPrefix()
                                .append(Component.text("Console cannot have bypass. Please specify a player name.", NamedTextColor.RED))
                );
            }
            return;
        }
        
        // Check if trying to bypass for another player
        String targetPlayerName = args[1];
        
        // Check permission for bypassing others
        if (!source.hasPermission("beaconlabs.viasc.bypass.others")) {
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("You don't have permission to bypass other players", NamedTextColor.RED))
            );
            return;
        }
          // Find target player
        Optional<Player> targetPlayer = server.getPlayer(targetPlayerName);
        if (targetPlayer.isPresent()) {
            toggleBypass(source, targetPlayer.get().getUsername(), targetPlayer.get().getUniqueId());
        } else {
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("Player not found: ", NamedTextColor.RED))
                            .append(Component.text(targetPlayerName, NamedTextColor.GOLD))
            );
        }
    }
    
    /**
     * Toggle bypass status for a player
     */
    private void toggleBypass(CommandSource source, String playerName, UUID playerUuid) {
        if (plugin.hasBypass(playerUuid)) {
            plugin.removeBypass(playerUuid);
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("Version checking ", NamedTextColor.GOLD))
                            .append(Component.text("enabled", NamedTextColor.GREEN))
                            .append(Component.text(" for ", NamedTextColor.GOLD))
                            .append(Component.text(playerName, NamedTextColor.AQUA))
            );
        } else {
            plugin.addBypass(playerUuid);
            source.sendMessage(
                    plugin.getPrefix()
                            .append(Component.text("Version checking ", NamedTextColor.GOLD))
                            .append(Component.text("bypassed", NamedTextColor.RED))
                            .append(Component.text(" for ", NamedTextColor.GOLD))
                            .append(Component.text(playerName, NamedTextColor.AQUA))
            );
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
    
    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        String[] args = invocation.arguments();
        
        if (args.length == 0 || args.length == 1) {
            if (invocation.source().hasPermission("vsc.reload")) {
                suggestions.add("reload");
            }
            if (invocation.source().hasPermission("beaconlabs.viasc.bypass")) {
                suggestions.add("bypass");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bypass") && 
                invocation.source().hasPermission("beaconlabs.viasc.bypass.others")) {
            server.getAllPlayers().forEach(player -> suggestions.add(player.getUsername()));
        }
        
        return CompletableFuture.completedFuture(suggestions);
    }
}
