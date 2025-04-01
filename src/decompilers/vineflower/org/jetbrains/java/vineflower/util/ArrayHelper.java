package org.jetbrains.java.vineflower.util;

public class ArrayHelper {
  public static <T> boolean containsByRef(T[] array, T value) {
    for (T t : array) {
      if (t == value) {
        return true;
      }
    }

    return false;
  }
}
