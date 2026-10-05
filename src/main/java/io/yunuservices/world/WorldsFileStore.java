package io.yunuservices.world;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class WorldsFileStore {

    private static final String STORAGE_ROOT = "tracked-worlds";
    private static final char SEPARATOR = '/';

    private final Plugin plugin;
    private final Scheduler scheduler;
    private final Path file;
    private final Object lock = new Object();
    private final Object writeLock = new Object();
    private final AtomicBoolean flushScheduled = new AtomicBoolean();
    private YamlConfiguration configuration;
    private String pendingSnapshot;

    public WorldsFileStore(final Plugin plugin, final Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.file = plugin.getDataFolder().toPath().resolve("worlds.yml");
        this.reload();
    }

    public void reload() {
        synchronized (this.lock) {
            try {
                Files.createDirectories(this.plugin.getDataFolder().toPath());
            } catch (final IOException ex) {
                throw new IllegalStateException("Failed to create the plugin data folder.", ex);
            }

            this.configuration = Files.exists(this.file)
                ? YamlConfiguration.loadConfiguration(this.file.toFile())
                : new YamlConfiguration();
            this.configuration.options().pathSeparator(SEPARATOR);

            if (!this.configuration.isConfigurationSection(STORAGE_ROOT)) {
                this.configuration.createSection(STORAGE_ROOT);
                this.persistAsync();
            }
        }
    }

    public boolean isTracked(final String worldName) {
        synchronized (this.lock) {
            return this.configuration.isConfigurationSection(this.worldPath(worldName));
        }
    }

    public List<String> trackedWorldNames() {
        synchronized (this.lock) {
            final ConfigurationSection section = this.configuration.getConfigurationSection(STORAGE_ROOT);
            if (section == null) {
                return List.of();
            }

            final List<String> result = new ArrayList<>();
            for (final String key : section.getKeys(false)) {
                if (section.isConfigurationSection(key) && WorldNameRules.normalize(key).isPresent()) {
                    result.add(key);
                }
            }
            result.sort(String.CASE_INSENSITIVE_ORDER);
            return result;
        }
    }

    public World.Environment environment(final String worldName) {
        synchronized (this.lock) {
            final String environmentName = this.configuration.getString(this.worldPath(worldName) + "/environment");
            if (environmentName == null || environmentName.isBlank()) {
                return null;
            }

            try {
                return World.Environment.valueOf(environmentName.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ex) {
                this.plugin.getLogger().warning("Stored invalid environment '" + environmentName + "' for world '" + worldName + "'.");
                return null;
            }
        }
    }

    public void rememberEnvironment(final String worldName, final World.Environment environment) {
        synchronized (this.lock) {
            final String normalizedName = this.requireWorldName(worldName);
            final String path = this.worldPath(normalizedName);
            final String value = environment.name();
            if (value.equals(this.configuration.getString(path + "/environment"))
                && normalizedName.equals(this.configuration.getString(path + "/name"))) {
                return;
            }

            this.configuration.set(path + "/name", normalizedName);
            this.configuration.set(path + "/environment", value);
            this.persistAsync();
        }
    }

    public void rememberSpawn(final String worldName, final Location location) {
        synchronized (this.lock) {
            final String normalizedName = this.requireWorldName(worldName);
            final String path = this.worldPath(normalizedName);
            if (this.hasSameSpawn(path, normalizedName, location)) {
                return;
            }
            this.configuration.set(path + "/name", normalizedName);
            this.configuration.set(path + "/spawn/x", location.getX());
            this.configuration.set(path + "/spawn/y", location.getY());
            this.configuration.set(path + "/spawn/z", location.getZ());
            this.configuration.set(path + "/spawn/yaw", location.getYaw());
            this.configuration.set(path + "/spawn/pitch", location.getPitch());
            this.persistAsync();
        }
    }

    public Difficulty difficulty(final String worldName) {
        synchronized (this.lock) {
            final String difficultyName = this.configuration.getString(this.worldPath(worldName) + "/difficulty");
            if (difficultyName == null || difficultyName.isBlank()) {
                return null;
            }

            try {
                return Difficulty.valueOf(difficultyName.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ex) {
                this.plugin.getLogger().warning("Stored invalid difficulty '" + difficultyName + "' for world '" + worldName + "'.");
                return null;
            }
        }
    }

    public void rememberDifficulty(final String worldName, final Difficulty difficulty) {
        synchronized (this.lock) {
            final String normalizedName = this.requireWorldName(worldName);
            final String path = this.worldPath(normalizedName);
            final String value = difficulty.name();
            if (value.equals(this.configuration.getString(path + "/difficulty"))
                && normalizedName.equals(this.configuration.getString(path + "/name"))) {
                return;
            }

            this.configuration.set(path + "/name", normalizedName);
            this.configuration.set(path + "/difficulty", value);
            this.persistAsync();
        }
    }

    public GameMode gameMode(final String worldName) {
        synchronized (this.lock) {
            final String gameModeName = this.configuration.getString(this.worldPath(worldName) + "/game-mode");
            if (gameModeName == null || gameModeName.isBlank()) {
                return null;
            }

            try {
                return GameMode.valueOf(gameModeName.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException ex) {
                this.plugin.getLogger().warning("Stored invalid game mode '" + gameModeName + "' for world '" + worldName + "'.");
                return null;
            }
        }
    }

    public void rememberGameMode(final String worldName, final GameMode gameMode) {
        synchronized (this.lock) {
            final String normalizedName = this.requireWorldName(worldName);
            final String path = this.worldPath(normalizedName);
            final String value = gameMode.name();
            if (value.equals(this.configuration.getString(path + "/game-mode"))
                && normalizedName.equals(this.configuration.getString(path + "/name"))) {
                return;
            }

            this.configuration.set(path + "/name", normalizedName);
            this.configuration.set(path + "/game-mode", value);
            this.persistAsync();
        }
    }

    public String difficultySummary(final String worldName) {
        final Difficulty difficulty = this.difficulty(worldName);
        return difficulty == null ? "-" : difficulty.name();
    }

    public String gameModeSummary(final String worldName) {
        final GameMode gameMode = this.gameMode(worldName);
        return gameMode == null ? "-" : gameMode.name();
    }

    public void trackWorld(final String worldName, final World.Environment environment, final Location spawn) {
        synchronized (this.lock) {
            final String normalizedName = this.requireWorldName(worldName);
            final String path = this.worldPath(normalizedName);
            this.configuration.set(path + "/name", normalizedName);
            if (environment != null) {
                this.configuration.set(path + "/environment", environment.name());
            }
            if (spawn != null) {
                this.configuration.set(path + "/spawn/x", spawn.getX());
                this.configuration.set(path + "/spawn/y", spawn.getY());
                this.configuration.set(path + "/spawn/z", spawn.getZ());
                this.configuration.set(path + "/spawn/yaw", spawn.getYaw());
                this.configuration.set(path + "/spawn/pitch", spawn.getPitch());
            }
            this.persistAsync();
        }
    }

    public void copyWorldSettings(final String sourceWorldName, final String targetWorldName, final World.Environment fallbackEnvironment) {
        synchronized (this.lock) {
            final String normalizedSourceName = this.requireWorldName(sourceWorldName);
            final String normalizedTargetName = this.requireWorldName(targetWorldName);
            final String sourcePath = this.worldPath(normalizedSourceName);
            final String targetPath = this.worldPath(normalizedTargetName);

            this.configuration.set(targetPath + "/name", normalizedTargetName);

            final String environment = this.configuration.getString(sourcePath + "/environment");
            if (environment != null && !environment.isBlank()) {
                this.configuration.set(targetPath + "/environment", environment);
            } else if (fallbackEnvironment != null) {
                this.configuration.set(targetPath + "/environment", fallbackEnvironment.name());
            }

            if (this.configuration.isConfigurationSection(sourcePath + "/spawn")) {
                this.configuration.set(targetPath + "/spawn/x", this.configuration.getDouble(sourcePath + "/spawn/x"));
                this.configuration.set(targetPath + "/spawn/y", this.configuration.getDouble(sourcePath + "/spawn/y"));
                this.configuration.set(targetPath + "/spawn/z", this.configuration.getDouble(sourcePath + "/spawn/z"));
                this.configuration.set(targetPath + "/spawn/yaw", (float) this.configuration.getDouble(sourcePath + "/spawn/yaw"));
                this.configuration.set(targetPath + "/spawn/pitch", (float) this.configuration.getDouble(sourcePath + "/spawn/pitch"));
            }

            this.copySection(sourcePath + "/portals", targetPath + "/portals");

            this.persistAsync();
        }
    }

    public String portalWorld(final String worldName, final PortalKind portalKind) {
        synchronized (this.lock) {
            return this.configuration.getString(this.portalPath(worldName, portalKind) + "/world");
        }
    }

    public ServerTransferTarget portalTransfer(final String worldName, final PortalKind portalKind) {
        synchronized (this.lock) {
            final String value = this.configuration.getString(this.portalPath(worldName, portalKind) + "/transfer");
            if (value == null || value.isBlank()) {
                return null;
            }

            try {
                return ServerTransferTarget.parse(value);
            } catch (final IllegalArgumentException ex) {
                this.plugin.getLogger().warning(
                    "Stored invalid transfer target '" + value + "' for world '" + worldName + "' and portal " + portalKind.displayName() + "."
                );
                return null;
            }
        }
    }

    public String portalWorldSummary(final String worldName, final PortalKind portalKind) {
        final String value = this.portalWorld(worldName, portalKind);
        return value == null || value.isBlank() ? "-" : value;
    }

    public String portalTransferSummary(final String worldName, final PortalKind portalKind) {
        final ServerTransferTarget transferTarget = this.portalTransfer(worldName, portalKind);
        return transferTarget == null ? "-" : transferTarget.asConfigValue();
    }

    public void rememberPortalWorld(final String worldName, final PortalKind portalKind, final String targetWorldName) {
        synchronized (this.lock) {
            final String normalizedName = this.requireWorldName(worldName);
            final String path = this.portalPath(normalizedName, portalKind);
            if (targetWorldName.equals(this.configuration.getString(path + "/world"))
                && normalizedName.equals(this.configuration.getString(this.worldPath(normalizedName) + "/name"))) {
                return;
            }

            this.configuration.set(this.worldPath(normalizedName) + "/name", normalizedName);
            this.configuration.set(path + "/world", targetWorldName);
            this.persistAsync();
        }
    }

    public void clearPortalWorld(final String worldName, final PortalKind portalKind) {
        synchronized (this.lock) {
            final String path = this.portalPath(worldName, portalKind) + "/world";
            if (!this.configuration.contains(path)) {
                return;
            }

            this.configuration.set(path, null);
            this.persistAsync();
        }
    }

    public void rememberPortalTransfer(final String worldName, final PortalKind portalKind, final ServerTransferTarget transferTarget) {
        synchronized (this.lock) {
            final String normalizedName = this.requireWorldName(worldName);
            final String path = this.portalPath(normalizedName, portalKind);
            final String value = transferTarget.asConfigValue();
            if (value.equals(this.configuration.getString(path + "/transfer"))
                && normalizedName.equals(this.configuration.getString(this.worldPath(normalizedName) + "/name"))) {
                return;
            }

            this.configuration.set(this.worldPath(normalizedName) + "/name", normalizedName);
            this.configuration.set(path + "/transfer", value);
            this.persistAsync();
        }
    }

    public void clearPortalTransfer(final String worldName, final PortalKind portalKind) {
        synchronized (this.lock) {
            final String path = this.portalPath(worldName, portalKind) + "/transfer";
            if (!this.configuration.contains(path)) {
                return;
            }

            this.configuration.set(path, null);
            this.persistAsync();
        }
    }

    public Location resolveSpawn(final World world) {
        synchronized (this.lock) {
            final String path = this.worldPath(world.getName()) + "/spawn";
            final Location fallback = world.getSpawnLocation();
            if (!this.configuration.isConfigurationSection(path)) {
                return fallback;
            }

            return new Location(
                world,
                this.configuration.getDouble(path + "/x", fallback.getX()),
                this.configuration.getDouble(path + "/y", fallback.getY()),
                this.configuration.getDouble(path + "/z", fallback.getZ()),
                (float) this.configuration.getDouble(path + "/yaw", fallback.getYaw()),
                (float) this.configuration.getDouble(path + "/pitch", fallback.getPitch())
            );
        }
    }

    public String spawnSummary(final String worldName) {
        synchronized (this.lock) {
            final String path = this.worldPath(worldName) + "/spawn";
            if (!this.configuration.isConfigurationSection(path)) {
                return "-";
            }

            return String.format(
                Locale.US,
                "%.1f, %.1f, %.1f (yaw %.1f, pitch %.1f)",
                this.configuration.getDouble(path + "/x"),
                this.configuration.getDouble(path + "/y"),
                this.configuration.getDouble(path + "/z"),
                this.configuration.getDouble(path + "/yaw"),
                this.configuration.getDouble(path + "/pitch")
            );
        }
    }

    public void removeWorld(final String worldName) {
        synchronized (this.lock) {
            final String path = this.worldPath(worldName);
            if (!this.configuration.contains(path)) {
                return;
            }

            this.configuration.set(path, null);
            this.persistAsync();
        }
    }

    private String worldPath(final String worldName) {
        return STORAGE_ROOT + SEPARATOR + this.requireWorldName(worldName);
    }

    private String portalPath(final String worldName, final PortalKind portalKind) {
        return this.worldPath(worldName) + "/portals/" + portalKind.configKey();
    }

    private String requireWorldName(final String worldName) {
        return WorldNameRules.normalize(worldName)
            .orElseThrow(() -> new IllegalArgumentException("Invalid managed world name: " + worldName));
    }

    private void persistAsync() {
        synchronized (this.lock) {
            this.pendingSnapshot = this.configuration.saveToString();
        }
        this.scheduleFlush();
    }

    private void scheduleFlush() {
        if (!this.flushScheduled.compareAndSet(false, true)) {
            return;
        }
        this.scheduler.executeAsync(this.plugin, () -> {
            try {
                this.writePendingSnapshot();
            } finally {
                this.flushScheduled.set(false);
                if (this.hasPendingSnapshot()) {
                    this.scheduleFlush();
                }
            }
        });
    }

    public void flush() {
        this.writePendingSnapshot();
    }

    private void writePendingSnapshot() {
        synchronized (this.writeLock) {
            final String snapshot = this.takePendingSnapshot();
            if (snapshot == null) {
                return;
            }

            final Path temp = this.file.resolveSibling(this.file.getFileName() + ".tmp");
            try {
                Files.writeString(temp, snapshot, StandardCharsets.UTF_8);
                try {
                    Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (final AtomicMoveNotSupportedException ex) {
                    Files.move(temp, this.file, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (final IOException ex) {
                this.plugin.getLogger().warning("Failed to persist worlds.yml: " + ex.getMessage());
            }
        }
    }

    private String takePendingSnapshot() {
        synchronized (this.lock) {
            final String snapshot = this.pendingSnapshot;
            this.pendingSnapshot = null;
            return snapshot;
        }
    }

    private boolean hasPendingSnapshot() {
        synchronized (this.lock) {
            return this.pendingSnapshot != null;
        }
    }

    private boolean hasSameSpawn(final String path, final String worldName, final Location location) {
        if (!this.configuration.isConfigurationSection(path + "/spawn")) {
            return false;
        }
        if (!worldName.equals(this.configuration.getString(path + "/name"))) {
            return false;
        }
        return Double.compare(this.configuration.getDouble(path + "/spawn/x"), location.getX()) == 0
            && Double.compare(this.configuration.getDouble(path + "/spawn/y"), location.getY()) == 0
            && Double.compare(this.configuration.getDouble(path + "/spawn/z"), location.getZ()) == 0
            && Float.compare((float) this.configuration.getDouble(path + "/spawn/yaw"), location.getYaw()) == 0
            && Float.compare((float) this.configuration.getDouble(path + "/spawn/pitch"), location.getPitch()) == 0;
    }

    private void copySection(final String sourcePath, final String targetPath) {
        this.configuration.set(targetPath, null);
        final ConfigurationSection section = this.configuration.getConfigurationSection(sourcePath);
        if (section == null) {
            return;
        }

        for (final String key : section.getKeys(true)) {
            if (section.isConfigurationSection(key)) {
                continue;
            }
            this.configuration.set(targetPath + SEPARATOR + key, section.get(key));
        }
    }
}
