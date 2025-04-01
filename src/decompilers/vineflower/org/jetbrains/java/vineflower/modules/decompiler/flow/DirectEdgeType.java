package org.jetbrains.java.vineflower.modules.decompiler.flow;

public enum DirectEdgeType {
  REGULAR,
  EXCEPTION;

  // read-only!
  public static final DirectEdgeType[] TYPES = values();
}
