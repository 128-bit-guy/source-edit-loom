// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.vineflower.struct.attr;

import org.jetbrains.java.vineflower.code.BytecodeVersion;
import org.jetbrains.java.vineflower.struct.consts.ConstantPool;
import org.jetbrains.java.vineflower.util.DataInputFullStream;

import java.io.IOException;

public class StructGenericSignatureAttribute extends StructGeneralAttribute {

  private String signature;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    int index = data.readUnsignedShort();
    signature = pool.getPrimitiveConstant(index).getString();
  }

  public String getSignature() {
    return signature;
  }
}
