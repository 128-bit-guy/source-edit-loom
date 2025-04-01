package org.jetbrains.java.vineflower.modules.decompiler.exps;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Pattern {
  @NotNull List<VarExprent> getPatternVars();
}
