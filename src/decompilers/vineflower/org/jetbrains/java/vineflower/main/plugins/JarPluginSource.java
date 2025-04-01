package org.jetbrains.java.vineflower.main.plugins;

import org.jetbrains.java.vineflower.api.plugin.Plugin;
import org.jetbrains.java.vineflower.api.plugin.PluginSource;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class JarPluginSource implements PluginSource {
    public JarPluginSource() {
    }

    public List<Plugin> findPlugins() {
      List<Plugin> plugins = new ArrayList<>();
      for (Class<?> cl : JarPluginLoader.PLUGIN_CLASSES) {
        try {
          plugins.add((Plugin) cl.getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
          e.printStackTrace();
        }
      }

      return plugins;
    }
}
