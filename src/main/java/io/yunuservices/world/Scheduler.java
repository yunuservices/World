package io.yunuservices.world;

import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

public interface Scheduler {

    void executeGlobal(Plugin plugin, Runnable task);

    void executeAsync(Plugin plugin, Runnable task);

    void executeRegion(Plugin plugin, World world, int chunkX, int chunkZ, Runnable task);

    void executeEntity(Plugin plugin, Entity entity, Runnable task, Runnable retired, long delay);
}
