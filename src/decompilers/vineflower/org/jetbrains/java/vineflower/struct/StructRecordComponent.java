// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.vineflower.struct;

import org.jetbrains.java.vineflower.code.BytecodeVersion;
import org.jetbrains.java.vineflower.main.DecompilerContext;
import org.jetbrains.java.vineflower.main.extern.IFernflowerPreferences;
import org.jetbrains.java.vineflower.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.vineflower.struct.attr.StructGenericSignatureAttribute;
import org.jetbrains.java.vineflower.struct.consts.ConstantPool;
import org.jetbrains.java.vineflower.struct.consts.PrimitiveConstant;
import org.jetbrains.java.vineflower.struct.gen.generics.GenericFieldDescriptor;
import org.jetbrains.java.vineflower.struct.gen.generics.GenericMain;
import org.jetbrains.java.vineflower.util.DataInputFullStream;

import java.io.IOException;
import java.util.Map;

/*
  record_component_info {
    u2 name_index;
    u2 descriptor_index;
    u2 attributes_count;
    attribute_info attributes[attributes_count];
   }
*/
public class StructRecordComponent extends StructField {
  public static StructRecordComponent create(DataInputFullStream in, ConstantPool pool, BytecodeVersion version) throws IOException {
    int nameIndex = in.readUnsignedShort();
    int descriptorIndex = in.readUnsignedShort();

    String name = ((PrimitiveConstant)pool.getConstant(nameIndex)).getString();
    String descriptor = ((PrimitiveConstant)pool.getConstant(descriptorIndex)).getString();

    Map<String, StructGeneralAttribute> attributes = readAttributes(in, pool, version);
    GenericFieldDescriptor signature = null;
    if (DecompilerContext.getOption(IFernflowerPreferences.DECOMPILE_GENERIC_SIGNATURES)) {
      StructGenericSignatureAttribute signatureAttr = (StructGenericSignatureAttribute)attributes.get(StructGeneralAttribute.ATTRIBUTE_SIGNATURE.name);
      if (signatureAttr != null) {
        signature = GenericMain.parseFieldSignature(signatureAttr.getSignature());
      }
    }

    return new StructRecordComponent(0, attributes, name, descriptor, signature, version);
  }

  private StructRecordComponent(int flags, Map<String, StructGeneralAttribute> attributes, String name, String descriptor, GenericFieldDescriptor signature, BytecodeVersion version) {
    super(flags, attributes, name, descriptor, signature, version);
  }
}
