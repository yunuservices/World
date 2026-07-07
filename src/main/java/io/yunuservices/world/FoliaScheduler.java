package io.yunuservices.world;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class FoliaScheduler implements Scheduler {

    @Override
    public void executeGlobal(final Plugin plugin, final Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void executeAsync(final Plugin plugin, final Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, ignored -> task.run());
    }

    @Override
    public void executeEntity(final Plugin plugin, final Entity entity, final Runnable task, final Runnable retired, final long delay) {
        entity.getScheduler().execute(plugin, task, retired, delay);
    }
}
