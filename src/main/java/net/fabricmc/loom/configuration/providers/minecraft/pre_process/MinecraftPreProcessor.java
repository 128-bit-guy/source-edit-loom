package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import com.google.common.base.Predicates;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.DependencyInfo;
import net.fabricmc.loom.util.Pair;

import org.objectweb.asm.tree.TypeInsnNode;

public class MinecraftPreProcessor {
	private final ConfigContext configContext;
	private final Path inputPath;
	private final Path outputPath;
	private final String libraryConfiguration;

	public MinecraftPreProcessor(
			ConfigContext configContext,
			Path inputPath,
			Path outputPath,
			String libraryConfiguration) {
		this.configContext = configContext;
		this.inputPath = inputPath;
		this.outputPath = outputPath;
		this.libraryConfiguration = libraryConfiguration;
	}

	private void loadLibraries(LibraryConsumer result) throws IOException {
		result.loadLibrary(this.inputPath);
		Configuration configuration = configContext.project().getConfigurations()
				.getByName(libraryConfiguration);
		System.out.println("Dependencies for inheritance tree:");
		for (Dependency dependency : configuration.getDependencies()) {
			Path p = DependencyInfo.create(configContext.project(), dependency, configuration)
					.resolveFile().get().toPath();
			result.loadLibrary(p);
			System.out.println(dependency.getName());
		}
	}

	private ClassInheritanceTree getClassInheritanceTree() throws IOException {
		ClassInheritanceTree result = new ClassInheritanceTree(5);
		loadLibraries(result);
		return result;
	}

	private MethodData getMethodData(ClassInheritanceTree classInheritanceTree) throws IOException {
		MethodData result = new MethodData(classInheritanceTree);
		loadLibraries(result);
		return result;
	}

	public void process() throws IOException {
		if (Files.exists(outputPath)) {
			Files.delete(outputPath);
		}
		System.out.println("Preprocessing " + inputPath + " to " + outputPath);
		Files.copy(inputPath, outputPath);
		ClassInheritanceTree classInheritanceTree = getClassInheritanceTree();
		System.out.println("LCA of LWJGL Exception and NullPointerException is: "
				+ classInheritanceTree
				.getLowestCommonAncestor("org/lwjgl/LWJGLException", "java/lang/NullPointerException"));
		MethodData methodData = getMethodData(classInheritanceTree);

//		System.out.println("Exceptions of data output stream write short: " + methodExceptionData.getMethodExceptions(new MethodIdentifier("java/io/DataOutputStream", "writeShort", "(I)V")));

		MinecraftJarGlobalProcessor processor = new MinecraftJarGlobalProcessor(outputPath);
		Queue<Pair<MethodIdentifier, String>> addedExceptionQueue = new ArrayDeque<>();
		Map<MethodIdentifier, List<Pair<MethodIdentifier, List<String>>>> methodCalls = new HashMap<>();

		// Fill map of method callers and add initial things to BFS queue
		for (ClassNode node : processor.classes.values()) {
			for (MethodNode method : node.methods) {
				MethodIdentifier caller = MethodIdentifier.fromMethodNode(node.name, method);
				Map<LabelNode, List<TryCatchBlockNode>> tryCatchBegins = new HashMap<>();
				Map<LabelNode, List<TryCatchBlockNode>> tryCatchEnds = new HashMap<>();
				Set<TryCatchBlockNode> currentTryCatchBlocks = new HashSet<>();
				for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
					tryCatchBegins.computeIfAbsent(tryCatch.start, k -> new ArrayList<>()).add(tryCatch);
					tryCatchEnds.computeIfAbsent(tryCatch.end, k -> new ArrayList<>()).add(tryCatch);
				}
				TypeInsnNode lastNewExceptionNode = null;
				for (AbstractInsnNode instruction : method.instructions) {
					if (instruction instanceof LabelNode label) {
						if (tryCatchBegins.containsKey(label)) {
							currentTryCatchBlocks.addAll(tryCatchBegins.get(label));
						}
						if (tryCatchEnds.containsKey(label)) {
							tryCatchEnds.get(label).forEach(currentTryCatchBlocks::remove);
						}
					} else if (instruction instanceof MethodInsnNode m) {
						MethodIdentifier called = methodData.getDeclaredMethod(MethodIdentifier.fromMethodInsnNode(m));
						List<String> caughtExceptions = currentTryCatchBlocks
								.stream()
								.map(block -> block.type)
								.filter(Predicates.notNull())
								.toList();
						methodCalls.computeIfAbsent(called, k -> new ArrayList<>())
								.add(new Pair<>(caller, caughtExceptions));
						Optional<List<String>> exceptions = methodData.getMethodExceptions(called);
						if(exceptions.isPresent()) {
							for (String thrownException : exceptions.get()) {
								boolean uncaught = caughtExceptions
										.stream()
										.noneMatch(ex -> classInheritanceTree.isAncestor(thrownException, ex));
								if (uncaught) {
									addedExceptionQueue.add(new Pair<>(caller, thrownException));
								}
							}
						}
					} else if (instruction instanceof TypeInsnNode typeInsn) {
						if (typeInsn.getOpcode() != Opcodes.NEW) continue;
						if (!classInheritanceTree.isAncestor(typeInsn.desc, "java/lang/Exception")) continue;
						if (classInheritanceTree.isAncestor(typeInsn.desc, "java/lang/RuntimeException")) continue;
						lastNewExceptionNode = typeInsn;
					} else {
						if(instruction.getOpcode() != Opcodes.ATHROW) continue;
						if(lastNewExceptionNode == null) continue;
						List<String> caughtExceptions = currentTryCatchBlocks
								.stream()
								.map(block -> block.type)
								.filter(Predicates.notNull())
								.toList();
						// For absolute correctness a stack should be used, but this works in most cases
						String thrownException = lastNewExceptionNode.desc;
						boolean uncaught = caughtExceptions
								.stream()
								.noneMatch(ex -> classInheritanceTree.isAncestor(thrownException, ex));
						if(uncaught) {
							addedExceptionQueue.add(new Pair<>(caller, thrownException));
						}
					}
				}
			}
		}

		// Run BFS, propagating thrown exceptions
		while (!addedExceptionQueue.isEmpty()) {
			Pair<MethodIdentifier, String> pair = addedExceptionQueue.poll();
			MethodIdentifier methodId = pair.left();
			String exception = pair.right();

			// Add exception to the method signature, if not already added
			ClassNode cl = processor.classes.get(methodId.className());
			if(cl == null) continue;
			MethodNode method = cl.methods.stream().filter(methodId::checkMethodNode).findAny().orElse(null);
			if(method == null) continue;
			boolean alreadyDeclared = method.exceptions
					.stream()
					.anyMatch(ex -> classInheritanceTree.isAncestor(exception, ex));
			if(alreadyDeclared) continue;
			method.exceptions.add(exception);

			// Propagate exception to all methods which call this
			List<Pair<MethodIdentifier, List<String>>> calls = methodCalls.get(methodId);
			if(calls != null) {
				for(Pair<MethodIdentifier, List<String>> call : calls) {
					MethodIdentifier callerId = call.left();
					List<String> caughtExceptions = call.right();
					boolean caught = caughtExceptions
							.stream()
							.anyMatch(ex -> classInheritanceTree.isAncestor(exception, ex));
					if(caught) continue;
					addedExceptionQueue.add(new Pair<>(callerId, exception));
				}
			}

			// Propagate exception to all methods from which this method is inherited
			if(method.name.equals("<init>") || method.name.equals("<clinit>")) continue;
			Set<String> interfacesToCheck = new HashSet<>();
			String currentClass = methodId.className();
			while(
					processor.classes.containsKey(currentClass)
							&& (methodId.className().equals(currentClass)
							|| !methodData.doesMethodExist(methodId.withClassName(currentClass)))
			) {
				processor.addAllInheritedInterfaces(currentClass, interfacesToCheck);
				currentClass = classInheritanceTree.getParent(currentClass, 0);
			}
			for(String interfaceName : interfacesToCheck) {
				MethodIdentifier propagatedId = methodId.withClassName(interfaceName);
				if(!methodData.doesMethodExist(propagatedId)) continue;
				addedExceptionQueue.add(new Pair<>(propagatedId, exception));
			}
			if(processor.classes.containsKey(currentClass)) {
				ClassNode otherClass = processor.classes.get(currentClass);
				MethodNode m = otherClass.methods
						.stream()
						.filter(methodId::checkMethodNode)
						.findAny()
						.orElse(null);
				if(m == null) continue;
				if((m.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) continue;
				MethodIdentifier propagatedId = methodId.withClassName(currentClass);
				addedExceptionQueue.add(new Pair<>(propagatedId, exception));
			}
		}

		processor.save();


//		try (FileSystemUtil.Delegate jar = FileSystemUtil.getJarFileSystem(outputPath);
//			 Stream<Path> jarStream = Files.walk(jar.getPath("/"))) {
//			for (Path f : jarStream.toList()) {
//				if (Files.isDirectory(f)) {
//					continue;
//				}
//				if (!f.getFileName().toString().endsWith(".class")) {
//					continue;
//				}
//				ClassReader classReader = new ClassReader(Files.readAllBytes(f));
//				ClassNode classNode = new ClassNode();
//				classReader.accept(classNode, 0);
//				for (MethodNode methodNode : classNode.methods) {
//					for (String exception : methodNode.exceptions) {
//						System.out.println("THROWS EXCEPTION: " + exception);
//					}
//					methodNode.exceptions.add("java/io/IOException");
//				}
//				ClassWriter classWriter = new ClassWriter(0);
//				classNode.accept(classWriter);
//				Files.write(f, classWriter.toByteArray());
//			}
//		}
	}
}
