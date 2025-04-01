package org.jetbrains.java.vineflower.api.plugin;

import org.jetbrains.java.vineflower.main.ClassesProcessor;
import org.jetbrains.java.vineflower.main.collectors.ImportCollector;
import org.jetbrains.java.vineflower.main.rels.ClassWrapper;
import org.jetbrains.java.vineflower.struct.StructClass;
import org.jetbrains.java.vineflower.struct.StructField;
import org.jetbrains.java.vineflower.struct.StructMethod;
import org.jetbrains.java.vineflower.util.TextBuffer;

public interface StatementWriter {
  void writeClassHeader(StructClass cl, TextBuffer buffer, ImportCollector importCollector);

  void writeClass(ClassesProcessor.ClassNode node, TextBuffer buffer, int indent);

  void writeField(ClassWrapper wrapper, StructClass cl, StructField fd, TextBuffer buffer, int indent);

  boolean writeMethod(ClassesProcessor.ClassNode node, StructMethod mt, int methodIndex, TextBuffer buffer, int indent);
}
