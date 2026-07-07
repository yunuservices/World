# World

`World` is a world manager plugin for **Paper** and **Canvas** servers. It uses the Canvas asynchronous world unload API when available, falls back to standard Bukkit world management on Paper, and provides Cloud Paper commands with async-safe scheduling.

## Requirements

- Java 25
- Paper 1.21+ **or** Canvas 26.1.2+
- Folia is **not supported** (the plugin disables itself on Folia)

[![CI](https://github.com/yunuservices/World/actions/workflows/ci.yml/badge.svg)](https://github.com/yunuservices/World/actions/workflows/ci.yml)

## Features

- Create, load, unload, import, copy, delete, and inspect worlds
- Runtime-aware world management (asynchronous unload on Canvas, synchronous fallback on Paper)
- World spawn management
- Portal routing between loaded worlds
- Cross-server transfer routing through portal events
- Async command suggestions for worlds and players
- Paper and Canvas runtime support

## Commands

| Command | Description |
| --- | --- |
| `/world list` | List known worlds from disk, memory, and tracked metadata. |
| `/world info <name>` | Show details for a world, including tracked portal rules. |
| `/world create <name> [environment] [seed]` | Create and load a new world. |
| `/world load <name> [environment]` | Load an existing world from disk. |
| `/world unload <name> [save]` | Unload a loaded world. Uses the async API on Canvas and the synchronous API on Paper. |
| `/world delete <name> [save]` | Unload and delete a world folder. |
| `/world untrack <name>` | Remove a world from `worlds.yml` without deleting its files. |
| `/world import <name> [environment]` | Track an existing world in `worlds.yml`. |
| `/world copy <source> <target> [load]` | Safely copy a world. Loaded source worlds are unloaded before copy and restored after copy. |
| `/world tp <world> [player]` | Teleport yourself or another player to a world spawn. |
| `/world spawn` | Send yourself to the configured spawn of your current world. |
| `/world spawn <world>` | Send yourself to the configured spawn of another loaded world. |
| `/world spawn <world> <player>` | Send another player to a world spawn. |
| `/world setspawn` | Save your current location as the spawn of your current world. |
| `/world setspawn <world>` | Save the Bukkit spawn location of a loaded world into `worlds.yml`. |
| `/world setspawn <world> <x> <y> <z> [yaw] [pitch]` | Set an explicit stored spawn for a loaded world. |
| `/world set portal <world> <NETHER\|END> <target-world\|off>` | Route a portal type to another loaded world or clear the rule. |
| `/world set transfer <world> <NETHER\|END> <host[:port]\|off>` | Route a portal type to another server or clear the rule. |
| `/world reload` | Reload `config.yml` and `worlds.yml` from disk. |

## Permissions

| Permission | Description |
| --- | --- |
| `world.*` | All plugin permissions. |
| `world.command.*` | All command permissions. |
| `world.command.list` | `/world list` |
| `world.command.info` | `/world info` |
| `world.command.create` | `/world create` |
| `world.command.load` | `/world load` |
| `world.command.unload` | `/world unload` |
| `world.command.delete` | `/world delete` |
| `world.command.untrack` | `/world untrack` |
| `world.command.import` | `/world import` |
| `world.command.copy` | `/world copy` |
| `world.command.tp` | `/world tp` |
| `world.command.tp.other` | Target another player with `/world tp` |
| `world.command.spawn` | `/world spawn` |
| `world.command.spawn.other` | Target another player with `/world spawn` |
| `world.command.set.*` | All `/world set ...` subcommands |
| `world.command.set.portal` | `/world set portal` |
| `world.command.set.transfer` | `/world set transfer` |
| `world.command.setspawn` | `/world setspawn` |
| `world.command.reload` | `/world reload` |

All permissions default to `op` unless you override them with a permission manager such as LuckPerms.

## Configuration

Default `config.yml`:

```yaml
defaults:
  environment: NORMAL
  generate-structures: true
  hardcore: false

commands:
  unload:
    save-by-default: true
  delete:
    save-by-default: true
  copy:
    load-copied-world-by-default: true
  import:
    capture-spawn-for-loaded-worlds: true
```

Initial `worlds.yml`:

```yaml
worlds: {}
```

Example tracked world entry:

```yaml
worlds:
  arena:
    name: arena
    environment: NORMAL
    spawn:
      x: 0.0
      y: 64.0
      z: 0.0
      yaw: 0.0
      pitch: 0.0
    portals:
      nether:
        world: arena_nether
        transfer: pvp.example.net:25565
      end:
        world: arena_end
```

## Building

This project requires **Java 25**. Make sure `JAVA_HOME` points to a Java 25 installation before building.

```bash
./gradlew build --no-daemon
```

Windows:

```powershell
.\gradlew.bat build --no-daemon
```

The shaded output jar is written to:

```text
build/libs/World-1.0.0-SNAPSHOT.jar
```

## Notes

- `copy` is conservative: if the source world is loaded, the plugin unloads it before copying and reloads it afterward.
- Cross-server transfer depends on runtime support for `Player#transfer`.
- **Canvas** uses the asynchronous `unloadWorldAsync` API for world unloads.
- **Paper** uses the standard synchronous `Bukkit.unloadWorld` API. Unloading large worlds may cause a short tick freeze.
- **Folia** is intentionally unsupported because it lacks an asynchronous world unload API. The plugin will disable itself on Folia with a console message.
- Licensed under the GNU Affero General Public License v3.0.
