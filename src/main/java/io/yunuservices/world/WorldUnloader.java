package io.yunuservices.world;

import java.util.function.Consumer;

public interface WorldUnloader {

    void unload(String name, boolean save, Consumer<UnloadResult> callback);
}
