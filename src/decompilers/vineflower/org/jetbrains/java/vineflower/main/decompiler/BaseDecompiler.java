// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.vineflower.main.decompiler;

import org.jetbrains.java.vineflower.main.Fernflower;
import org.jetbrains.java.vineflower.main.extern.IBytecodeProvider;
import org.jetbrains.java.vineflower.main.extern.IContextSource;
import org.jetbrains.java.vineflower.main.extern.IFernflowerLogger;
import org.jetbrains.java.vineflower.main.extern.IResultSaver;

import java.io.File;
import java.util.Map;

@SuppressWarnings("unused")
public class BaseDecompiler {
  private final Fernflower engine;

  public BaseDecompiler(IResultSaver saver, Map<String, Object> options, IFernflowerLogger logger) {
    engine = new Fernflower(saver, options, logger);
  }

  @Deprecated
  public BaseDecompiler(IBytecodeProvider provider, IResultSaver saver, Map<String, Object> options, IFernflowerLogger logger) {
    engine = new Fernflower(provider, saver, options, logger);
  }

  public void addSource(IContextSource source) {
    engine.addSource(source);
  }

  public void addSource(File source) {
    engine.addSource(source);
  }

  public void addLibrary(IContextSource source) {
    engine.addLibrary(source);
  }

  public void addLibrary(File library) {
    engine.addLibrary(library);
  }

  public void decompileContext() {
    try {
      engine.decompileContext();
    }
    finally {
      engine.clearContext();
    }
  }
}