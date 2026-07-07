package io.yunuservices.world;

import io.canvasmc.canvas.WorldUnloadResult;
import java.util.function.Consumer;
import org.bukkit.Bukkit;

public final class CanvasWorldUnloader implements WorldUnloader {

    @Override
    public void unload(final String name, final boolean save, final Consumer<UnloadResult> callback) {
        Bukkit.getServer().unloadWorldAsync(name, save, result -> callback.accept(this.map(result)));
    }

    private UnloadResult map(final WorldUnloadResult result) {
        return switch (result) {
            case SUCCESS -> UnloadResult.SUCCESS;
            case FAIL_PLAYERS_JOINING -> UnloadResult.FAIL_PLAYERS_JOINING;
            case FAIL_PLAYERS_PRESENT -> UnloadResult.FAIL_PLAYERS_PRESENT;
            case FAIL_ALREADY_UNLOADING -> UnloadResult.FAIL_ALREADY_UNLOADING;
            case FAIL_IS_OVERWORLD -> UnloadResult.FAIL_IS_OVERWORLD;
            case FAIL_UNLOAD_EVENT -> UnloadResult.FAIL_UNLOAD_EVENT;
            case FAIL_IS_SHUTDOWN -> UnloadResult.FAIL_IS_SHUTDOWN;
            case FAIL_UNKNOWN -> UnloadResult.FAIL_UNKNOWN;
        };
    }
}
