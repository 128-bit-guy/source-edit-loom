// Copyright 2019 FabricMC project. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package net.fabricmc.fernflower.vf_api;

import org.jetbrains.java.vineflower.main.ClassWriter;
import org.jetbrains.java.vineflower.main.Fernflower;
import org.jetbrains.java.vineflower.struct.StructClass;
import org.jetbrains.java.vineflower.struct.StructField;
import org.jetbrains.java.vineflower.struct.StructMethod;

/**
 * Provides (optional) javadoc for Classes/Methods/Fields encountered by
 *  {@link ClassWriter}.
 *
 * May be set as a property in the constructor of {@link Fernflower} by using
 *  the key {@code IFabricJavadocProvider.PROPERTY_NAME}
 */
public interface IFabricJavadocProvider {
  String PROPERTY_NAME = "fabric:javadoc";

  String getClassDoc(StructClass structClass);

  String getFieldDoc(StructClass structClass, StructField structField);

  String getMethodDoc(StructClass structClass, StructMethod structMethod);
}