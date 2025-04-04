package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import net.fabricmc.loom.util.FileSystemUtil;

/**
 * Data structure which allows to navigate class inheritance tree using binary lifting
 */
public class ClassInheritanceTree implements LibraryConsumer {
	private final int levels;
	private final List<Map<String, String>> parents;
	private final Map<String, Integer> heights;
	private final Map<String, Boolean> isInterface;

	/**
	 * Creates {@link ClassInheritanceTree} with specified number of levels. For the structure to properly function
	 * {@code 2^levels} must be bigger or equal to the maximum height of class
	 * @param levels number of levels in data structure
	 */
	public ClassInheritanceTree(int levels) {
		this.levels = levels;
		parents = new ArrayList<>(levels);
		for (int i = 0; i < levels; i++) {
			parents.add(new HashMap<>());
		}
		heights = new HashMap<>();
		parents.get(0).put("java/lang/Object", "java/lang/Object");
		heights.put("java/lang/Object", 0);
		isInterface = new HashMap<>();
	}

	/**
	 * Adds library from path to inheritance tree
	 * @param jarPath path to the library
	 */
	@Override
	public void loadLibrary(Path jarPath) throws IOException {
		try (FileSystemUtil.Delegate delegate = FileSystemUtil.getJarFileSystem(jarPath);
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
				boolean itf = (classNode.access & Opcodes.ACC_INTERFACE) != 0;
				isInterface.put(classNode.name, itf);
				if (itf) continue;
				parents.get(0).put(classNode.name, classNode.superName);
			}
		}
	}

	private String resolveParent(String className, int level) {
		if (level == 0) {
			// The class is probably from java standard library, ger parent using reflection
			String javaName = className.replace('/', '.');
			try {
				Class c = ClassInheritanceTree.class.getClassLoader().loadClass(javaName);
				if(c.getSuperclass() == null) {
					return "java/lang/Object";
				}
				String parent = c.getSuperclass().getName();
				return parent.replace('.', '/');
			} catch (ClassNotFoundException e) {
				System.err.println("Failed to resolve parent for class " + javaName);
				e.printStackTrace();
				return "java/lang/Object";
			}
		} else {
			// Lazily build binary lifting data structure
			return getParent(getParent(className, level - 1), level - 1);
		}
	}

	/**
	 * Finds the superclass of the specified class at a level of {@code 2^level} steps up
	 * in the inheritance hierarchy.
	 *
	 * @param className 	name of the class in internal java format
	 *                     	(meaning package names and class name are separated by slashes)
	 * @param level         the exponent in {@code 2^level} determining how many steps up
	 *                   	the hierarchy to traverse.  Must be non-negative and not bigger
	 *                   	than {@code levels} parameter passed in constructor
	 * @return superclass of the specified class at a level of {@code 2^level} steps up in the inheritance hierarchy
	 */
	public String getParent(String className, int level) {
		if (parents.get(level).containsKey(className)) {
			return parents.get(level).get(className);
		}
		String parent = resolveParent(className, level);
		parents.get(level).put(className, parent);
		return parent;
	}

	/**
	 * Finds the number of steps in path from class {@code className} to {@link Object} in the inheritance hierarchy
	 * @param className 	name of the class in internal java format
	 *                      (meaning package names and class name are separated by slashes)
	 * @return the number of steps in path from this class to {@link Object} in the inheritance hierarchy
	 */
	public int getHeight(String className) {
		if(heights.containsKey(className)) {
			return heights.get(className);
		}
		int height = getHeight(getParent(className, 0)) + 1;
		heights.put(className, height);
		return height;
	}

	private String getAncestorOfHeight(String className, int height) {
		for(int i = levels - 1; i >= 0; --i) {
			String parent = getParent(className, i);
			if(getHeight(parent) >= height) {
				className = parent;
			}
		}
		return className;
	}

	/**
	 * Finds the lowest common ancestor of classes {@code classA} and {@code classB},
	 * meaning the class with maximum height which is common ancestor of both classes
	 * @return the lowest common ancestor of classes {@code classA} and {@code classB}
	 */
	public String getLowestCommonAncestor(String classA, String classB) {
		if(getHeight(classA) > getHeight(classB)) {
			String temp = classA;
			classA = classB;
			classB = temp;
		}
		classB = getAncestorOfHeight(classB, getHeight(classA));
		if(classA.equals(classB)) {
			return classA;
		}
		for(int i = levels - 1; i >= 0; --i) {
			String parentA = getParent(classA, i);
			String parentB = getParent(classB, i);
			if(!parentA.equals(parentB)) {
				classA = parentA;
				classB = parentB;
			}
		}
		return getParent(classA, 0);
	}

	/**
	 * Checks whether {@code ancestor} is an ancestor of {@code className}
	 */
	public boolean isAncestor(String className, String ancestor) {
		String ancestorOfHeight = getAncestorOfHeight(className, getHeight(ancestor));
		return ancestorOfHeight.equals(ancestor);
	}

	private boolean resolveIsInterface(String className) {
		String javaName = className.replace('/', '.');
		try {
			Class c = ClassInheritanceTree.class.getClassLoader().loadClass(javaName);
			return c.isInterface();
		} catch (ClassNotFoundException e) {
			System.err.println("Failed to resolve parent for class " + javaName);
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Checks whether {@code className} is interface
	 */
	public boolean isInterface(String className) {
		if(isInterface.containsKey(className)) {
			return isInterface.get(className);
		}
		boolean itf = resolveIsInterface(className);
		isInterface.put(className, itf);
		return itf;
	}
}
