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
        if (runtimeType == RuntimeType.FOLIA) {
            this.getLogger().severe("Folia is not supported because it lacks an asynchronous world unload API. "
                + "Please consider using Paper/Canvas or their forks.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        final Scheduler scheduler = runtimeType == RuntimeType.CANVAS
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
        Bukkit.getPluginManager().registerEvents(new WorldGameModeListener(this.worldsFileStore), this);
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

        String displayName() {
            return switch (this) {
                case CANVAS -> "Canvas";
                case FOLIA -> "Folia";
                case PAPER -> "Paper";
            };
        }
    }
}
