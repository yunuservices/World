package io.yunuservices.world;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private PluginConfigStore configStore;
    private WorldsFileStore worldsFileStore;
    private MessagesStore messagesStore;

    @Override
    public void onEnable() {
        final RuntimeType runtimeType = this.detectRuntime();
        final Scheduler scheduler = runtimeType.hasFoliaScheduler()
            ? new FoliaScheduler()
            : new BukkitScheduler();
        final WorldUnloader worldUnloader = runtimeType == RuntimeType.CANVAS
            ? new CanvasWorldUnloader()
            : new BukkitWorldUnloader();

        this.messagesStore = new MessagesStore(this);
        this.configStore = new PluginConfigStore(this);
        this.worldsFileStore = new WorldsFileStore(this, scheduler);

        final WorldManagerServiceImpl service = new WorldManagerServiceImpl(
            this,
            this.configStore,
            this.worldsFileStore,
            this.messagesStore,
            scheduler,
            worldUnloader
        );
        Bukkit.getPluginManager().registerEvents(new WorldPortalListener(this, this.worldsFileStore, this.messagesStore, scheduler), this);
        new WorldCommands(this, service).register();
        service.loadTrackedWorldsOnStartup();
        this.getLogger().info("World has been enabled. " + runtimeType.displayName() + " world manager is ready.");
    }

    public PluginConfigStore configStore() {
        return this.configStore;
    }

    public WorldsFileStore worldsFileStore() {
        return this.worldsFileStore;
    }

    public MessagesStore messagesStore() {
        return this.messagesStore;
    }

    private RuntimeType detectRuntime() {
        try {
            Class.forName("io.canvasmc.canvas.WorldUnloadResult");
            Bukkit.getServer().getClass().getMethod("unloadWorldAsync", String.class, boolean.class, java.util.function.Consumer.class);
            return RuntimeType.CANVAS;
        } catch (final ReflectiveOperationException ignored) {
        }

        try {
            Bukkit.class.getMethod("getGlobalRegionScheduler");
            return RuntimeType.FOLIA;
        } catch (final ReflectiveOperationException ignored) {
        }

        return RuntimeType.PAPER;
    }

    private enum RuntimeType {
        CANVAS,
        FOLIA,
        PAPER;

        boolean hasFoliaScheduler() {
            return this == CANVAS || this == FOLIA;
        }

        String displayName() {
            return switch (this) {
                case CANVAS -> "Canvas";
                case FOLIA -> "Folia";
                case PAPER -> "Paper";
            };
        }
    }
}
