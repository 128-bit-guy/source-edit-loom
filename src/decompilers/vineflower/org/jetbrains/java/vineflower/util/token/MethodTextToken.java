package org.jetbrains.java.vineflower.util.token;

import org.jetbrains.java.vineflower.main.extern.TextTokenVisitor;
import org.jetbrains.java.vineflower.struct.gen.MethodDescriptor;

public class MethodTextToken extends TextToken {
  public final String className;
  public final String name;
  public final MethodDescriptor descriptor;

  public MethodTextToken(int start, int length, boolean declaration, String className, String name, MethodDescriptor descriptor) {
    super(start, length, declaration);
    this.className = className;
    this.name = name;
    this.descriptor = descriptor;
  }

  @Override
  public MethodTextToken copy() {
    return new MethodTextToken(start, length, declaration, className, name, descriptor);
  }

  @Override
  public void visit(TextTokenVisitor visitor) {
    visitor.visitMethod(new TextRange(start, length), declaration, className, name, descriptor);
  }
}
