package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import net.fabricmc.loom.util.FileSystemUtil;

public class MethodData implements LibraryConsumer {
	private final Map<MethodIdentifier, Optional<List<String>>> methodExceptions;
	private final Map<MethodIdentifier, Optional<MethodIdentifier>> declaredMethods;
	private final ClassInheritanceTree classInheritanceTree;

	public MethodData(ClassInheritanceTree classInheritanceTree) {
		this.classInheritanceTree = classInheritanceTree;
		this.methodExceptions = new HashMap<>();
		this.declaredMethods = new HashMap<>();
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
					methodExceptions.put(id, Optional.of(methodNode.exceptions));
				}
			}
		}
	}

	private Optional<List<String>> resolveMethodExceptions(MethodIdentifier methodIdentifier) {
		String javaClassName = methodIdentifier.className().replace('/', '.');
		try {
			Class c = MethodData.class.getClassLoader().loadClass(javaClassName);
			for (Method m : c.getDeclaredMethods()) {
				if (!methodIdentifier.checkMethod(m)) continue;
				Class[] exceptions = m.getExceptionTypes();
				return Optional.of(Arrays.stream(exceptions)
						.map(ex -> ex.getName().replace('.', '/'))
						.collect(Collectors.toList()));
			}
			for (Constructor ctor : c.getDeclaredConstructors()) {
				if (!methodIdentifier.checkConstructor(ctor)) continue;
				Class[] exceptions = ctor.getExceptionTypes();
				return Optional.of(Arrays.stream(exceptions)
						.map(ex -> ex.getName().replace('.', '/'))
						.collect(Collectors.toList()));
			}
			return Optional.empty();
//			System.err.println("Failed to resolve method " + methodIdentifier);
		} catch (ClassNotFoundException e) {
//			System.err.println("Failed to resolve class " + javaClassName);
//			e.printStackTrace();
			return Optional.empty();
		}
	}

	public Optional<List<String>> getMethodExceptions(MethodIdentifier methodIdentifier) {
		if (methodExceptions.containsKey(methodIdentifier)) {
			return methodExceptions.get(methodIdentifier);
		}
		Optional<List<String>> result = resolveMethodExceptions(methodIdentifier);
		methodExceptions.put(methodIdentifier, result);
		return result;
	}

	public boolean doesMethodExist(MethodIdentifier methodIdentifier) {
		return getMethodExceptions(methodIdentifier).isPresent();
	}

	private Optional<MethodIdentifier> resolveDeclaredMethod(MethodIdentifier methodIdentifier) {
		if (doesMethodExist(methodIdentifier)) {
			return Optional.of(methodIdentifier);
		}
		String currentClass = methodIdentifier.className();
		if (currentClass.equals("java/lang/Object")) {
			return Optional.empty();
		}
		String parent = classInheritanceTree.getParent(currentClass, 0);
		MethodIdentifier parentIdentifier =
				new MethodIdentifier(parent, methodIdentifier.methodName(), methodIdentifier.methodDesc());
		return getDeclaredMethod0(parentIdentifier);
	}

	public MethodIdentifier getDeclaredMethod(MethodIdentifier methodIdentifier) {
		Optional<MethodIdentifier> identifier = getDeclaredMethod0(methodIdentifier);
		if(identifier.isEmpty()) {
			System.err.println("Method " + methodIdentifier.methodName() + " not found in any of parent classes");
			return methodIdentifier;
		}
		return identifier.get();
	}

	private Optional<MethodIdentifier> getDeclaredMethod0(MethodIdentifier methodIdentifier) {
		if (declaredMethods.containsKey(methodIdentifier)) {
			return declaredMethods.get(methodIdentifier);
		}
		Optional<MethodIdentifier> result = resolveDeclaredMethod(methodIdentifier);
		declaredMethods.put(methodIdentifier, result);
		return result;
	}
}
