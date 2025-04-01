package org.jetbrains.java.vineflower.util.token;

import org.jetbrains.java.vineflower.main.extern.TextTokenVisitor;

public class ClassTextToken extends TextToken {
  public final String qualifiedName;

  public ClassTextToken(int start, int length, boolean declaration, String qualifiedName) {
    super(start, length, declaration);
    this.qualifiedName = qualifiedName;
  }

  @Override
  public ClassTextToken copy() {
    return new ClassTextToken(start, length, declaration, qualifiedName);
  }

  @Override
  public void visit(TextTokenVisitor visitor) {
    visitor.visitClass(new TextRange(start, length), declaration, qualifiedName);
  }
}
