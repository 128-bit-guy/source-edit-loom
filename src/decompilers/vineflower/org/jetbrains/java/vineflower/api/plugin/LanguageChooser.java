package org.jetbrains.java.vineflower.api.plugin;

import org.jetbrains.java.vineflower.struct.StructClass;

public interface LanguageChooser {
  boolean isLanguage(StructClass cl);
}
