package org.bcnlab.viaServerConfig.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.ViaAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bcnlab.viaServerConfig.ViaServerConfig;

import java.util.HashMap;
import java.util.Map;

public class ServerConnectListener {
    private final ViaServerConfig plugin;
    private Map<String, ViaServerConfig.ServerRequirement> serverVersionRequirements = new HashMap<>();

    public ServerConnectListener(ViaServerConfig plugin) {
        this.plugin = plugin;
        reloadServerRequirements();
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        RegisteredServer target = event.getOriginalServer();
        String serverName = target.getServerInfo().getName();

        if (plugin.hasBypass(player.getUniqueId())) {
            return;
        }

        ViaServerConfig.ServerRequirement requirement = serverVersionRequirements.get(serverName);
        if (requirement == null || requirement.getTargetProtocolVersion() <= 0) {
            return;
        }

        try {
            ViaAPI<?> api = Via.getAPI();
            int playerVersion = api.getPlayerVersion(player.getUniqueId());

            if (!requirement.canConnect(playerVersion)) {
                event.setResult(ServerPreConnectEvent.ServerResult.denied());
                
                String rawMessage = plugin.getDenyMessage().replace("{needed_version}", getVersionName(requirement.getTargetProtocolVersion()));
                Component denyMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(rawMessage);
                
                player.sendMessage(plugin.getPrefix().append(denyMessage));
            }
        } catch (Exception e) {
            plugin.getLogger().error("Error checking player version", e);
        }
    }

    /**
     * Reload server version requirements from config
     */
    public void reloadServerRequirements() {
        serverVersionRequirements.clear();
        serverVersionRequirements.putAll(plugin.getServerVersionRequirements());
    }
    
    /**
     * Convert protocol version to a readable version name
     */
    private String getVersionName(int protocolVersion) {
        String versionName = plugin.getProtocolVersionName(protocolVersion);
        if (versionName != null) {
            return versionName;
        }
        
        return "Protocol " + protocolVersion;
    }
}
