// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.vineflower.struct.attr;

import org.jetbrains.java.vineflower.code.BytecodeVersion;
import org.jetbrains.java.vineflower.modules.decompiler.exps.Exprent;
import org.jetbrains.java.vineflower.struct.consts.ConstantPool;
import org.jetbrains.java.vineflower.util.DataInputFullStream;

import java.io.IOException;

public class StructAnnDefaultAttribute extends StructGeneralAttribute {

  private Exprent defaultValue;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    defaultValue = StructAnnotationAttribute.parseAnnotationElement(data, pool);
  }

  public Exprent getDefaultValue() {
    return defaultValue;
  }
}
