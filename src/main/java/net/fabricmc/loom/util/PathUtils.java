package net.fabricmc.loom.util;

import com.google.common.base.Predicates;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Predicate;

public class PathUtils {
	public static void copyContents(Path srcRoot, Path destRoot, Predicate<Path> predicate) throws IOException {
		Files.walkFileTree(srcRoot, new FileVisitor<Path>() {
			@Override
			public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) throws IOException {
				System.out.println("SrcRoot: " + srcRoot + ", dir: " + dir);
				Path partPath = srcRoot.relativize(dir);
				Path resPath = destRoot.resolve(partPath.toString());
				if(predicate.test(dir)) {
					Files.createDirectories(resPath);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
				Path resPath = destRoot.resolve(srcRoot.relativize(file).toString());
				if(predicate.test(file)) {
					System.out.println("In file: " + file.toString());
					System.out.println("Out file: " + resPath.toString());
					if(resPath.getParent() != null) {
						Files.createDirectories(resPath.getParent());
					}
					byte[] b = Files.readAllBytes(file);
					Files.write(resPath, b, StandardOpenOption.CREATE);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public @NotNull FileVisitResult visitFileFailed(Path file, @NotNull IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public @NotNull FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public static void copyContents(Path srcRoot, Path destRoot) throws IOException {
		copyContents(srcRoot, destRoot, Predicates.alwaysTrue());
	}
}
