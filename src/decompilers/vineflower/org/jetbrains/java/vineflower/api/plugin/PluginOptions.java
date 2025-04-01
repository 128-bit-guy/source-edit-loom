package org.jetbrains.java.vineflower.api.plugin;

import org.jetbrains.java.vineflower.util.Pair;

import java.util.function.Consumer;

@FunctionalInterface
public interface PluginOptions {
  Pair<Class<?>, Consumer<AddDefaults>> provideOptions();

  @FunctionalInterface
  interface AddDefaults {
    void addDefault(String key, Object defaultVal);
  }
}
