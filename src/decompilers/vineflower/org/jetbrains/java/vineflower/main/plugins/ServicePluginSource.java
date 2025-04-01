package org.jetbrains.java.vineflower.main.plugins;

import org.jetbrains.java.vineflower.api.plugin.Plugin;
import org.jetbrains.java.vineflower.api.plugin.PluginSource;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ServicePluginSource implements PluginSource {
    public List<Plugin> findPlugins() {
      List<Plugin> plugins = new ArrayList<>();
      for (Plugin plugin : ServiceLoader.load(Plugin.class, getClass().getClassLoader())) {
        plugins.add(plugin);
      }

      return plugins;
    }
}
