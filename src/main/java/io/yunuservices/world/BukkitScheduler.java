package io.yunuservices.world;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public final class BukkitScheduler implements Scheduler {

    @Override
    public void executeGlobal(final Plugin plugin, final Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public void executeAsync(final Plugin plugin, final Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
    }

    @Override
    public void executeEntity(final Plugin plugin, final Entity entity, final Runnable task, final Runnable retired, final long delay) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
