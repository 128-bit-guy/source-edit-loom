package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import net.fabricmc.loom.util.FileSystemUtil;

public class MethodExceptionData implements LibraryConsumer {
	private final Map<MethodIdentifier, List<String>> methodExceptions;

	public MethodExceptionData() {
		this.methodExceptions = new HashMap<>();
	}

	public void loadLibrary(Path libraryPath) throws IOException {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(libraryPath);
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
				for (MethodNode methodNode : classNode.methods) {
					MethodIdentifier id = MethodIdentifier.fromMethodNode(classNode.name, methodNode);
					methodExceptions.put(id, methodNode.exceptions);
				}
			}
		}
	}

	private List<String> resolveMethodExceptions(MethodIdentifier methodIdentifier) {
		String javaClassName = methodIdentifier.className().replace('/', '.');
		try {
			Class c = MethodExceptionData.class.getClassLoader().loadClass(javaClassName);
			for (Method m : c.getDeclaredMethods()) {
				if (!methodIdentifier.checkMethod(m)) continue;
				Class[] exceptions = m.getExceptionTypes();
				return Arrays.stream(exceptions)
						.map(ex -> ex.getName().replace('.', '/'))
						.collect(Collectors.toList());
			}
			System.err.println("Failed to resolve method " + methodIdentifier);
		} catch (ClassNotFoundException e) {
			System.err.println("Failed to resolve class " + javaClassName);
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	public List<String> getMethodExceptions(MethodIdentifier methodIdentifier) {
		if (methodExceptions.containsKey(methodIdentifier)) {
			return methodExceptions.get(methodIdentifier);
		}
		List<String> result = resolveMethodExceptions(methodIdentifier);
		methodExceptions.put(methodIdentifier, result);
		return result;
	}
}
