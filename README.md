# ViaServerConfig

A powerful Velocity plugin that manages Minecraft version compatibility across your network servers using ViaVersion.

## 📋 Overview

ViaServerConfig allows server administrators to precisely control which Minecraft versions are allowed to connect to each server in their network. With comprehensive configuration options, you can set specific version requirements per server and even grant bypass permissions to specific players.

## ⚙️ Setup

1. **Prerequisites**
   - Velocity Server
   - ViaVersion installed

2. **Installation**
   - Place the `viaserverconfig-1.0.jar` in your server's `plugins` folder
   - Restart your server
   - Configuration files will be automatically generated

## 🔧 Configuration

### Main Configuration (`config.yml`)

```yaml
# Custom prefix for plugin messages
prefix: '&6BeaconLabs &8» '

# Message displayed when a player is denied access
# Use {needed_version} as a placeholder for the required version
deny-message: '&cYou need Minecraft {needed_version} or newer to join this server!'

# Server-specific settings
servers:
    lobby: {target-protocol-version: 0, allow-over: true, allow-under: true}
    survival: {target-protocol-version: 760, allow-over: true, allow-under: false}
    minigames: {target-protocol-version: 758, minimum-protocol-version: 754, allow-over: true, allow-under: false}
```

### Protocol Version Mappings (`protocol.yml`)

This file maps protocol version numbers to human-readable Minecraft versions.

```yaml
versions:
  # Protocol: "Minecraft Version"
  47: "1.8.x"
  340: "1.12.2"
  758: "1.18.2"
  # etc.
```

## 🛠️ Server Configuration Settings in Detail

Each server configuration has several settings that control which Minecraft versions can connect:

| Setting | Description |
|---------|-------------|
| `target-protocol-version` | The protocol version players should ideally have. Set to `0` to disable version checking for this server. |
| `minimum-protocol-version` | The minimum protocol version required (optional). Players below this version will always be denied. |
| `allow-over` | Allow players with newer versions to join. |
| `allow-under` | Allow players with older versions to join. |

### How Version Control Works

The version control system follows this decision-making process:

1. **Bypass Check**: If a player has bypass permission, they are always allowed to connect regardless of version.

2. **Disabled Check**: If `target-protocol-version` is set to `0`, version checking is disabled for that server.

3. **Minimum Version Check**: If `minimum-protocol-version` is greater than `0`, any player with a version below this will be denied, regardless of other settings.

4. **Target Version Comparison**:
   - If player's version is **equal** to `target-protocol-version`: **ALLOWED**
   - If player's version is **greater** than `target-protocol-version`:
     - If `allow-over` is `true`: **ALLOWED**
     - If `allow-over` is `false`: **DENIED**
   - If player's version is **less** than `target-protocol-version`:
     - If `allow-under` is `true`: **ALLOWED**
     - If `allow-under` is `false`: **DENIED**

### Settings Explained with Examples

```yaml
# Example 1: No version restrictions (default)
server1: {target-protocol-version: 0, minimum-protocol-version: 0, allow-over: true, allow-under: true}
# Result: All players can connect regardless of version

# Example 2: Only 1.21.4 (protocol 769) and above
server2: {target-protocol-version: 769, minimum-protocol-version: 0, allow-over: true, allow-under: false}
# Result: Only players with 1.21.4 or newer can connect

# Example 3: Only exactly 1.21.4 (protocol 769)
server3: {target-protocol-version: 769, minimum-protocol-version: 0, allow-over: false, allow-under: false}
# Result: Only players with exactly 1.21.4 can connect

# Example 4: Versions between 1.18.2 (protocol 758) and 1.21.4 (protocol 769)
server4: {target-protocol-version: 769, minimum-protocol-version: 758, allow-over: false, allow-under: true}
# Result: Players with versions 1.18.2 up to (and including) 1.21.4 can connect

# Example 5: 1.18.2 (protocol 758) and above, but not newer than 1.21.4 (protocol 769)
server5: {target-protocol-version: 769, minimum-protocol-version: 758, allow-over: false, allow-under: true}
# Result: Same as Example 4, but using minimum-protocol-version to enforce the lower bound
```

### Priority and Overrides

When multiple settings might conflict:

1. **`minimum-protocol-version` overrides `allow-under`**: Even if you set `allow-under` to `true`, players below `minimum-protocol-version` will be denied.

2. **Exact version matches are always allowed**: If a player has exactly the `target-protocol-version`, they are allowed regardless of other settings.

3. **Target `0` disables all checks**: When `target-protocol-version` is `0`, both `minimum-protocol-version` and the allow settings are ignored.

## 🔑 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/vsc` | Show plugin info | None |
| `/vsc reload` | Reload configuration | `vsc.reload` |
| `/vsc bypass` | Toggle version check bypass for yourself | `beaconlabs.viasc.bypass` |
| `/vsc bypass <player>` | Toggle version check bypass for another player | `beaconlabs.viasc.bypass.others` |

## 🔒 Permissions

| Permission | Description |
|------------|-------------|
| `vsc.reload` | Allows reloading the plugin configuration |
| `beaconlabs.viasc.bypass` | Allows bypassing version checks for yourself |
| `beaconlabs.viasc.bypass.others` | Allows bypassing version checks for other players |

## 🔄 Automatic Features

- **Server Auto-Registration**: All servers are automatically added to the configuration when the plugin starts
- **Protocol Version Mapping**: Human-readable version names are used in messages instead of protocol numbers

## 🌟 Examples

### Requiring at least 1.18.2
```yaml
my_server: {target-protocol-version: 758, allow-over: true, allow-under: false}  # 1.18.2
```

### Only allowing exactly 1.16.5
```yaml
my_server: {target-protocol-version: 754, allow-over: false, allow-under: false}  # 1.16.5
```

### Allowing only versions between 1.16.5 and 1.19.4
```yaml
my_server: {target-protocol-version: 762, minimum-protocol-version: 754, allow-over: false, allow-under: true}  # Min: 1.16.5, Max: 1.19.4
```

## 💡 Tips

- Use protocol.yml to add new versions as they are released
- Grant bypass permissions to staff members for testing
- Use `/vsc reload` after making changes to apply them immediately
- Check the console for any loading errors

## 🐛 Troubleshooting

If you encounter problems:

1. Ensure ViaVersion is properly installed
2. Check that your configuration syntax is correct
3. Look for error messages in the console
4. Verify that protocol versions in your config match those in protocol.yml

## 👏 Credits

- **Author**: Vincent Wackler
- **Website**: bcnlab.org
