package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import net.fabricmc.loom.util.FileSystemUtil;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

public class MinecraftJarGlobalProcessor {
	public Map<String, ClassNode> classes = new HashMap<>();
	private final Path path;

	public MinecraftJarGlobalProcessor(Path path) throws IOException {
		this.path = path;
		loadJar();
	}

	private void loadJar() throws IOException {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(path);
			 Stream<Path> stream = Files.walk(delegate.getPath("/"))) {
			for (Path f : stream.toList()) {
				if (Files.isDirectory(f)) {
					continue;
				}
				if (!f.getFileName().toString().endsWith(".class")) {
					continue;
				}
				ClassReader classReader = new ClassReader(Files.readAllBytes(f));
				ClassNode classNode = new ClassNode();
				classReader.accept(classNode, 0);
				classes.put(classNode.name, classNode);
			}
		}
	}

	public void save() throws IOException {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(path)) {
			for (ClassNode classNode : classes.values()) {
				String className = classNode.name;
				Path p = delegate.getPath("/").resolve(className + ".class");
				ClassWriter writer = new ClassWriter(0);
				classNode.accept(writer);
				Files.write(p, writer.toByteArray());
			}
		}
	}

	public void addAllInheritedInterfaces(String className, Set<String> inheritedInterfaces) {
		Queue<String> queue = new ArrayDeque<>();
		queue.add(className);
		while (!queue.isEmpty()) {
			String current = queue.poll();
			if(inheritedInterfaces.contains(current)) continue;
			if(!classes.containsKey(current)) continue;
			inheritedInterfaces.add(current);
			queue.addAll(classes.get(current).interfaces);
		}
		inheritedInterfaces.remove(className);
	}
}
