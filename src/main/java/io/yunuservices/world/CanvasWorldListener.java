package io.yunuservices.world;

import io.canvasmc.canvas.event.EntityPortalAsyncEvent;
import io.canvasmc.canvas.event.EntityPostPortalAsyncEvent;
import io.canvasmc.canvas.event.EntityPostTeleportAsyncEvent;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

public final class CanvasWorldListener implements Listener {

    private final Plugin plugin;
    private final WorldsFileStore worldsFileStore;
    private final Scheduler scheduler;
    private final WorldPortalListener portalListener;
    private final WorldGameModeListener gameModeListener;

    public CanvasWorldListener(
        final Plugin plugin,
        final WorldsFileStore worldsFileStore,
        final Scheduler scheduler,
        final WorldPortalListener portalListener,
        final WorldGameModeListener gameModeListener
    ) {
        this.plugin = plugin;
        this.worldsFileStore = worldsFileStore;
        this.scheduler = scheduler;
        this.portalListener = portalListener;
        this.gameModeListener = gameModeListener;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(final EntityPortalAsyncEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        final PortalKind portalKind = PortalKind.fromPortalType(event.getPortalType());
        if (portalKind == null) {
            return;
        }

        final String sourceWorldName = event.getFrom().getName();
        final ServerTransferTarget transferTarget = this.worldsFileStore.portalTransfer(sourceWorldName, portalKind);
        if (transferTarget != null) {
            event.setCancelled(true);
            this.portalListener.transfer(player, transferTarget);
            return;
        }

        final String targetWorldName = this.worldsFileStore.portalWorld(sourceWorldName, portalKind);
        if (targetWorldName == null || targetWorldName.isBlank()) {
            return;
        }

        final World targetWorld = Bukkit.getWorld(targetWorldName);
        if (targetWorld == null) {
            this.plugin.getLogger().warning("Configured " + portalKind.configKey()
                + " portal target world '" + targetWorldName + "' is not loaded.");
            return;
        }
        event.setTo(targetWorld);
    }

    @EventHandler
    public void onPostPortal(final EntityPostPortalAsyncEvent event) {
        if (event.getEntity() instanceof final Player player && !event.getFrom().equals(event.getTo())) {
            this.applyGameMode(player, event.getTo());
        }
    }

    @EventHandler
    public void onPostTeleport(final EntityPostTeleportAsyncEvent event) {
        final World from = event.getFrom().getWorld();
        final World to = event.getTo().getWorld();
        if (event.getEntity() instanceof final Player player && to != null && !to.equals(from)) {
            this.applyGameMode(player, to);
        }
    }

    private void applyGameMode(final Player player, final World world) {
        this.scheduler.executeEntity(this.plugin, player, () -> this.gameModeListener.applyGameMode(player, world), null, 1L);
    }
}
