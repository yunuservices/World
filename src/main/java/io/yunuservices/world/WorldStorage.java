package io.yunuservices.world;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class WorldStorage {

    private static final String OVERWORLD = "overworld";
    private static final String NETHER = "the_nether";
    private static final String END = "the_end";

    private final Path container;
    private final String levelName;
    private final Path dimensionRoot;
    private final boolean dimensionLayout;

    public WorldStorage(final Path container, final String levelName) {
        this.container = container.toAbsolutePath().normalize();
        this.levelName = levelName;
        this.dimensionRoot = this.container.resolve(levelName).resolve("dimensions").resolve("minecraft");
        this.dimensionLayout = Files.isDirectory(this.dimensionRoot);
    }

    public Path container() {
        return this.container;
    }

    public Path resolve(final String worldName) {
        final String name = WorldNameRules.normalize(worldName)
            .orElseThrow(() -> new IllegalArgumentException("Invalid managed world name: " + worldName));
        final Path resolved = (this.dimensionLayout
            ? this.dimensionRoot.resolve(this.dimensionKey(name))
            : this.container.resolve(name)).toAbsolutePath().normalize();
        if (!resolved.startsWith(this.container)) {
            throw new IllegalArgumentException("Refusing to use a path outside the world container: " + name);
        }
        return resolved;
    }

    public boolean isWorldFolder(final Path directory) {
        if (!Files.isDirectory(directory)) {
            return false;
        }
        return this.dimensionLayout || Files.exists(directory.resolve("level.dat"));
    }

    public List<String> listWorldNames() throws IOException {
        final Path root = this.dimensionLayout ? this.dimensionRoot : this.container;
        if (!Files.isDirectory(root)) {
            return List.of();
        }

        try (Stream<Path> stream = Files.list(root)) {
            return stream
                .filter(this::isWorldFolder)
                .map(path -> this.worldName(path.getFileName().toString()))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        }
    }

    private String dimensionKey(final String worldName) {
        if (worldName.equals(this.levelName)) {
            return OVERWORLD;
        }
        if (worldName.equals(this.levelName + "_nether")) {
            return NETHER;
        }
        if (worldName.equals(this.levelName + "_the_end")) {
            return END;
        }
        return worldName.toLowerCase(Locale.ROOT);
    }

    private String worldName(final String directoryName) {
        if (!this.dimensionLayout) {
            return directoryName;
        }
        return switch (directoryName) {
            case OVERWORLD -> this.levelName;
            case NETHER -> this.levelName + "_nether";
            case END -> this.levelName + "_the_end";
            default -> directoryName;
        };
    }
}
