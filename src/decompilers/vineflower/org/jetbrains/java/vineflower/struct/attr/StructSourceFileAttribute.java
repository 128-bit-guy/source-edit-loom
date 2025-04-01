package org.jetbrains.java.vineflower.struct.attr;

import org.jetbrains.java.vineflower.code.BytecodeVersion;
import org.jetbrains.java.vineflower.struct.consts.ConstantPool;
import org.jetbrains.java.vineflower.util.DataInputFullStream;

import java.io.IOException;

/*
  SourceFile_attribute {
    u2 attribute_name_index;
    u4 attribute_length;
    u2 sourcefile_index;
  }
 */
public class StructSourceFileAttribute extends StructGeneralAttribute {
  private int constPoolIndex;

  @Override
  public void initContent(DataInputFullStream data, ConstantPool pool, BytecodeVersion version) throws IOException {
    constPoolIndex = data.readUnsignedShort();
  }

  public String getSourceFile(ConstantPool pool) {
    return pool.getPrimitiveConstant(constPoolIndex).getString();
  }
}
