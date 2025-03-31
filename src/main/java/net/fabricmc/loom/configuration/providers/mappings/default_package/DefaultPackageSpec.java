package net.fabricmc.loom.configuration.providers.mappings.default_package;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import net.fabricmc.loom.api.mappings.layered.MappingContext;
import net.fabricmc.loom.api.mappings.layered.spec.MappingsSpec;
import net.fabricmc.loom.util.FileSystemUtil;

public record DefaultPackageSpec(String newPackage) implements MappingsSpec<DefaultPackageLayerImpl> {

	@Override
	public DefaultPackageLayerImpl createLayer(MappingContext context) {
		Map<String, String> renamedClasses = new HashMap<>();
		for (Path p : context.minecraftProvider().getMinecraftJars()) {
			List<String> classes = getDefaultPackageClasses(p);
			for (String s : classes) {
				renamedClasses.put(s, newPackage.replace('.', '/') + "/" + s);
			}
		}
		context.intermediaryTree().get().getClasses().forEach(c -> renamedClasses.remove(c.getSrcName()));
		return new DefaultPackageLayerImpl(renamedClasses);
	}

	private List<String> getDefaultPackageClasses(Path jar) {
		try (FileSystemUtil.Delegate input = FileSystemUtil.getJarFileSystem(jar)) {
			List<String> classes = new ArrayList<>();
			Path root = input.getPath("/");
			try (Stream<Path> files = Files.list(root)) {
				for (Path file : files.toList()) {
					if (Files.isDirectory(file)) continue;
					String fileName = file.getFileName().toString();
					if (!fileName.endsWith(".class")) continue;
					classes.add(fileName.substring(0, fileName.length() - ".class".length()));
				}
			}
			return classes;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
