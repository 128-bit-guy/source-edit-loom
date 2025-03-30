package net.fabricmc.loom.configuration.providers.jar_mods;

import net.fabricmc.loom.util.fmj.ModEnvironment;

import java.nio.file.Path;

public record JarMod(String name, Path jarFile, ModEnvironment environment) {

}
