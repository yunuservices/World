package io.yunuservices.world;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public final class WorldGameModeListener implements Listener {

    private final WorldsFileStore worldsFileStore;

    public WorldGameModeListener(final WorldsFileStore worldsFileStore) {
        this.worldsFileStore = worldsFileStore;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(final PlayerJoinEvent event) {
        this.applyGameMode(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldChange(final PlayerChangedWorldEvent event) {
        this.applyGameMode(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRespawn(final PlayerRespawnEvent event) {
        final World world = event.getRespawnLocation().getWorld();
        if (world != null) {
            this.applyGameMode(event.getPlayer(), world);
        }
    }

    private void applyGameMode(final Player player, final World world) {
        final GameMode gameMode = this.worldsFileStore.gameMode(world.getName());
        if (gameMode == null) {
            return;
        }
        if (player.getGameMode() != gameMode) {
            player.setGameMode(gameMode);
        }
    }
}
