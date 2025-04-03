package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import java.io.IOException;
import java.nio.file.Path;

public interface LibraryConsumer {
	/**
	 * Adds library from path to the object
	 * @param libraryPath path to the library
	 */
	void loadLibrary(Path libraryPath) throws IOException;
}
