package io.yunuservices.world;

import org.bukkit.World;

public record WorldDescriptor(
    String name,
    boolean loaded,
    boolean existsOnDisk,
    boolean tracked,
    World.Environment environment,
    String difficulty,
    String gameMode,
    Integer playerCount,
    Boolean hardcore,
    Boolean generatesStructures,
    String configuredSpawn
) {
}
