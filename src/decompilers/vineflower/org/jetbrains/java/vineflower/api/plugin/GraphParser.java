package org.jetbrains.java.vineflower.api.plugin;

import org.jetbrains.java.vineflower.code.cfg.ControlFlowGraph;
import org.jetbrains.java.vineflower.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.vineflower.struct.StructMethod;

// Turns a control flow graph into a structured statement
public interface GraphParser {
  RootStatement createStatement(ControlFlowGraph graph, StructMethod mt);
}
