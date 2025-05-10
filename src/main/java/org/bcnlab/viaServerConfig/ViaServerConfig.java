package org.bcnlab.viaServerConfig;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bcnlab.viaServerConfig.command.VSCCommand;
import org.bcnlab.viaServerConfig.listener.ServerConnectListener;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Plugin(
    id = "viaserverconfig", 
    name = "ViaServerConfig", 
    version = "1.0", 
    url = "bcnlab.org", 
    authors = {"Vincent Wackler"},
    dependencies = {@Dependency(id = "viaversion")}
)
public class ViaServerConfig {
    private ConfigurationNode config;
    private ConfigurationLoader<?> loader;
    private ConfigurationNode protocolConfig;

    private String prefix;
    private String denyMessage;
    private final String version = "1.0";
    private Path configFile;
    private Path protocolFile;

    private final Set<UUID> bypassedPlayers = new HashSet<>();
    private final Map<Integer, String> protocolVersionNames = new HashMap<>();
    
    @Inject
    private ProxyServer server;
    
    @Inject
    private CommandManager commandManager;
    
    @Inject
    @DataDirectory
    private Path dataDirectory;
    
    @Inject
    private Logger logger;
    
    private ServerConnectListener connectListener;

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        configFile = dataDirectory.resolve("config.yml");
        protocolFile = dataDirectory.resolve("protocol.yml");

        loadConfig();
        
        // Command
        commandManager.register(commandManager.metaBuilder("vsc").plugin(this).build(), new VSCCommand(this));
        
        // Listener
        connectListener = new ServerConnectListener(this);
        server.getEventManager().register(this, connectListener);
        
        // Servers updated every boot
        updateServerEntriesInConfig();

        // Finished
        logger.info("ViaServerConfig was started.");
    }
    
    public void loadConfig() {
        try {
            if (!Files.exists(configFile)) {
                Files.createDirectories(dataDirectory);
                Files.copy(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("config.yml")), configFile);
            }

            loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .build();

            config = loader.load();

            if (config != null) {
                prefix = config.node("prefix").getString("&6BeaconLabs &8» ");
                denyMessage = config.node("deny-message").getString("&cYou need Minecraft {needed_version} or newer to join this server!");

                if (config.node("deny-message").virtual()) {
                    config.node("deny-message").set("&cYou need Minecraft {needed_version} or newer to join this server!");
                    loader.save(config);
                }
            } else {
                logger.error("Failed to load config: Configuration is null");
                prefix = "&4ConfigError &8» ";
                denyMessage = "&cYou need Minecraft {needed_version} or newer to join this server!";
            }

            loadProtocolVersions();
            
        } catch (IOException e) {
            logger.error("Failed to load config!", e);
            prefix = "&4ConfigError &8» ";
            denyMessage = "&cYou need Minecraft {needed_version} or newer to join this server!";
        }
    }
    
    private void loadProtocolVersions() {
        try {
            if (!Files.exists(protocolFile)) {
                Files.createDirectories(dataDirectory);
                Files.copy(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("protocol.yml")), protocolFile);
            }
            
            ConfigurationLoader<?> protocolLoader = YamlConfigurationLoader.builder()
                    .path(protocolFile)
                    .build();
            
            protocolConfig = protocolLoader.load();

            protocolVersionNames.clear();
            
            ConfigurationNode versionsNode = protocolConfig.node("versions");
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : versionsNode.childrenMap().entrySet()) {
                try {
                    int protocolVersion = Integer.parseInt(entry.getKey().toString());
                    String versionName = entry.getValue().getString();
                    if (versionName != null) {
                        protocolVersionNames.put(protocolVersion, versionName);
                    }
                } catch (NumberFormatException e) {
                    logger.error("Invalid protocol version: " + entry.getKey().toString(), e);
                }
            }
            
            logger.info("Loaded " + protocolVersionNames.size() + " protocol version mappings");
            
        } catch (IOException e) {
            logger.error("Failed to load protocol versions!", e);
        }
    }
    
    private void updateServerEntriesInConfig() {
        try {
            boolean changed = false;
            for (RegisteredServer registeredServer : server.getAllServers()) {
                String serverName = registeredServer.getServerInfo().getName();
                if (config.node("servers", serverName).virtual()) {
                    config.node("servers", serverName, "target-protocol-version").set(0);
                    config.node("servers", serverName, "allow-over").set(true);
                    config.node("servers", serverName, "allow-under").set(true);
                    changed = true;
                } else if (!config.node("servers", serverName, "target-protocol-version").virtual()) {
                    continue;
                }
            }
            
            if (changed) {
                loader.save(config);
                logger.info("Updated config with new server entries");
            }
            
        } catch (IOException e) {
            logger.error("Failed to update server entries in config", e);
        }
    }
    
    public void reloadConfig() {
        loadConfig();
        if (connectListener != null) {
            connectListener.reloadServerRequirements();
        }
        logger.info("Configuration reloaded");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("ViaServerConfig is shutting down.");
    }
    
    /**
     * Get all server version requirements from config
     */
    public Map<String, ServerRequirement> getServerVersionRequirements() {
        Map<String, ServerRequirement> requirements = new HashMap<>();
        ConfigurationNode serversNode = config.node("servers");
        
        try {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : serversNode.childrenMap().entrySet()) {
                String serverName = entry.getKey().toString();
                ConfigurationNode serverNode = entry.getValue();
                
                if (serverNode.node("target-protocol-version").virtual()) {
                    // Old format or corrupted entry
                    int protocolVersion = 0;
                    try {
                        protocolVersion = serverNode.getInt(0);
                    } catch (Exception e) {
                        // Not an integer value, ignore
                    }
                    requirements.put(serverName, new ServerRequirement(protocolVersion, true, false));
                } else {
                    // New format with comprehensive options
                    int targetVersion = serverNode.node("target-protocol-version").getInt(0);
                    boolean allowOver = serverNode.node("allow-over").getBoolean(true);
                    boolean allowUnder = serverNode.node("allow-under").getBoolean(true);
                    
                    requirements.put(serverName, new ServerRequirement(targetVersion, allowOver, allowUnder));
                }
            }
        } catch (Exception e) {
            logger.error("Error loading server requirements from config", e);
        }
        
        return requirements;
    }
    
    /**
     * Get protocol version name mapping
     */
    public String getProtocolVersionName(int protocolVersion) {
        return protocolVersionNames.get(protocolVersion);
    }
    
    /**
     * Add a player to the bypass list
     */
    public void addBypass(UUID playerUuid) {
        bypassedPlayers.add(playerUuid);
    }
    
    /**
     * Remove a player from the bypass list
     */
    public void removeBypass(UUID playerUuid) {
        bypassedPlayers.remove(playerUuid);
    }
    
    /**
     * Check if a player has version check bypass
     */
    public boolean hasBypass(UUID playerUuid) {
        return bypassedPlayers.contains(playerUuid);
    }

    /**
     * Get all bypassed players
     */
    public Set<UUID> getBypassedPlayers() {
        return new HashSet<>(bypassedPlayers);
    }

    public Component getPrefix() {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix);
    }
    
    public String getDenyMessage() {
        return denyMessage;
    }

    public String getVersion() {
        return version;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    /**
     * Server requirement class to store comprehensive settings
     */
    public static class ServerRequirement {
        private final int targetProtocolVersion;
        private final boolean allowOver;
        private final boolean allowUnder;
        
        public ServerRequirement(int targetProtocolVersion, boolean allowOver, boolean allowUnder) {
            this.targetProtocolVersion = targetProtocolVersion;
            this.allowOver = allowOver;
            this.allowUnder = allowUnder;
        }
        
        public int getTargetProtocolVersion() {
            return targetProtocolVersion;
        }
        
        public boolean isAllowOver() {
            return allowOver;
        }
        
        public boolean isAllowUnder() {
            return allowUnder;
        }
        
        /**
         * Check if a player with the given version can connect
         */
        public boolean canConnect(int playerVersion) {
            // If no requirement (target is 0), always allow
            if (targetProtocolVersion <= 0) {
                return true;
            }
            
            if (playerVersion > targetProtocolVersion) {
                return allowOver;
            } else if (playerVersion < targetProtocolVersion) {
                return allowUnder;
            } else {
                // Exact match
                return true;
            }
        }
    }
}
