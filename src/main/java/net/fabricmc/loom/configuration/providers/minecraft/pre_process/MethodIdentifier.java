package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public record MethodIdentifier(String className, String methodName, String methodDesc) {
	private static String getDescriptorForClass(final Class c)
	{
		if(c.isPrimitive())
		{
			if(c==byte.class)
				return "B";
			if(c==char.class)
				return "C";
			if(c==double.class)
				return "D";
			if(c==float.class)
				return "F";
			if(c==int.class)
				return "I";
			if(c==long.class)
				return "J";
			if(c==short.class)
				return "S";
			if(c==boolean.class)
				return "Z";
			if(c==void.class)
				return "V";
			throw new RuntimeException("Unrecognized primitive "+c);
		}
		if(c.isArray()) return c.getName().replace('.', '/');
		return ('L'+c.getName()+';').replace('.', '/');
	}

	private static String getMethodDescriptor(Method m)
	{
		String s="(";
		for(final Class c: m.getParameterTypes())
			s+=getDescriptorForClass(c);
		s+=')';
		return s+getDescriptorForClass(m.getReturnType());
	}

	private static String getConstructorDescriptor(Constructor m)
	{
		String s="(";
		for(final Class c: m.getParameterTypes())
			s+=getDescriptorForClass(c);
		s+=')';
		return s+"V";
	}

	public boolean checkMethod(Method method) {

		if(!methodName.equals(method.getName())) return false;
		return getMethodDescriptor(method).equals(methodDesc);
	}

	public boolean checkMethodNode(MethodNode mn) {
		return methodName.equals(mn.name) && methodDesc.equals(mn.desc);
	}

	public boolean checkConstructor(Constructor c) {
		if(!methodName.equals("<init>")) return false;
		return getConstructorDescriptor(c).equals(methodDesc);
	}

	public static MethodIdentifier fromMethodNode(String className, MethodNode methodNode) {
		return new MethodIdentifier(className, methodNode.name, methodNode.desc);
	}

	public static MethodIdentifier fromMethodInsnNode(MethodInsnNode methodInsnNode) {

		return new MethodIdentifier(methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc);
	}
}
