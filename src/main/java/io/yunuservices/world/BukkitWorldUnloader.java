package io.yunuservices.world;

import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.World;

public final class BukkitWorldUnloader implements WorldUnloader {

    @Override
    @SuppressWarnings("removal")
    public void unload(final String name, final boolean save, final Consumer<UnloadResult> callback) {
        final World world = Bukkit.getWorld(name);
        if (world == null) {
            callback.accept(UnloadResult.SUCCESS);
            return;
        }
        final boolean hadPlayers = !world.getPlayers().isEmpty();
        final boolean success = Bukkit.unloadWorld(world, save);
        callback.accept(success ? UnloadResult.SUCCESS : this.guessFailure(hadPlayers));
    }

    private UnloadResult guessFailure(final boolean hadPlayers) {
        return hadPlayers ? UnloadResult.FAIL_PLAYERS_PRESENT : UnloadResult.FAIL_UNKNOWN;
    }
}
