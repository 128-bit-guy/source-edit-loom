package net.fabricmc.loom.configuration.providers.minecraft.pre_process;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.SimpleVerifier;

public class ClassInheritanceTreeBasedVerifier extends SimpleVerifier {
	private static final Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
	private final ClassInheritanceTree tree;

	public ClassInheritanceTreeBasedVerifier(ClassInheritanceTree tree) {
		this.tree = tree;
	}

	public ClassInheritanceTreeBasedVerifier(Type currentClass, Type currentSuperClass, boolean isInterface, ClassInheritanceTree tree) {
		super(currentClass, currentSuperClass, isInterface);
		this.tree = tree;
	}

	public ClassInheritanceTreeBasedVerifier(Type currentClass, Type currentSuperClass, List<Type> currentClassInterfaces, boolean isInterface, ClassInheritanceTree tree) {
		super(currentClass, currentSuperClass, currentClassInterfaces, isInterface);
		this.tree = tree;
	}

	public ClassInheritanceTreeBasedVerifier(int api, Type currentClass, Type currentSuperClass, List<Type> currentClassInterfaces, boolean isInterface, ClassInheritanceTree tree) {
		super(api, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
		this.tree = tree;
	}

	public ClassInheritanceTreeBasedVerifier(ClassNode classNode, ClassInheritanceTree tree) {
		this(
				Opcodes.ASM9,
				Type.getObjectType(classNode.name),
				Type.getObjectType(classNode.superName),
				classNode.interfaces.stream().map(Type::getObjectType).toList(),
				(classNode.access & Opcodes.ACC_INTERFACE) != 0,
				tree
		);
	}

	@Override
	public BasicValue merge(BasicValue value1, BasicValue value2) {
		Type type1 = value1.getType();
		Type type2 = value2.getType();
		// Null types correspond to BasicValue.UNINITIALIZED_VALUE.
		if (type1 == null || type2 == null) {
			return BasicValue.UNINITIALIZED_VALUE;
		}
		if (type1.equals(type2)) {
			return value1;
		}
		// The merge of a primitive type with a different type is the type of uninitialized values.
		if (type1.getSort() != Type.OBJECT && type1.getSort() != Type.ARRAY) {
			return BasicValue.UNINITIALIZED_VALUE;
		}
		if (type2.getSort() != Type.OBJECT && type2.getSort() != Type.ARRAY) {
			return BasicValue.UNINITIALIZED_VALUE;
		}
		// Special case for the type of the "null" literal.
		if (type1.equals(NULL_TYPE)) {
			return value2;
		}
		if (type2.equals(NULL_TYPE)) {
			return value1;
		}
		// Convert type1 to its element type and array dimension. Arrays of primitive values are seen as
		// Object arrays with one dimension less. Hence the element type is always of Type.OBJECT sort.
		int dim1 = 0;
		if (type1.getSort() == Type.ARRAY) {
			dim1 = type1.getDimensions();
			type1 = type1.getElementType();
			if (type1.getSort() != Type.OBJECT) {
				dim1 = dim1 - 1;
				type1 = OBJECT_TYPE;
			}
		}
		// Do the same for type2.
		int dim2 = 0;
		if (type2.getSort() == Type.ARRAY) {
			dim2 = type2.getDimensions();
			type2 = type2.getElementType();
			if (type2.getSort() != Type.OBJECT) {
				dim2 = dim2 - 1;
				type2 = OBJECT_TYPE;
			}
		}
		// The merge of array types of different dimensions is an Object array type.
		if (dim1 != dim2) {
			return newArrayValue(OBJECT_TYPE, Math.min(dim1, dim2));
		}
		// Type1 and type2 have a Type.OBJECT sort by construction (see above),
		// as expected by isAssignableFrom.
		if (isAssignableFrom(type1, type2)) {
			return newArrayValue(type1, dim1);
		}
		if (isAssignableFrom(type2, type1)) {
			return newArrayValue(type2, dim1);
		}
		if(isInterface(type1) || isInterface(type2)) {
			return newArrayValue(OBJECT_TYPE, dim1);
		}
		String commonType = tree.getLowestCommonAncestor(type1.getInternalName(), type2.getInternalName());
		return newArrayValue(Type.getObjectType(commonType), dim1);
	}

	private BasicValue newArrayValue(final Type type, final int dimensions) {
		if (dimensions == 0) {
			return newValue(type);
		} else {
			StringBuilder descriptor = new StringBuilder();
			for (int i = 0; i < dimensions; ++i) {
				descriptor.append('[');
			}
			descriptor.append(type.getDescriptor());
			return newValue(Type.getType(descriptor.toString()));
		}
	}

	@Override
	protected boolean isAssignableFrom(Type parent, Type child) {
		if (parent.equals(child)) {
			return true;
		}
		if(!isInterface(parent)) {
			if(isInterface(child)) {
				return parent.equals(OBJECT_TYPE);
			}
			return tree.isAncestor(child.getInternalName(), parent.getInternalName());
		}
		//TODO add support for checking interfaces
		return false;
	}

	@Override
	protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
		Type type = value.getType();
		Type expectedType = expected.getType();
		// Null types correspond to BasicValue.UNINITIALIZED_VALUE.
		if (type == null || expectedType == null) {
			return type == null && expectedType == null;
		}
		if (type.equals(expectedType)) {
			return true;
		}
		switch (expectedType.getSort()) {
			case Type.INT:
			case Type.FLOAT:
			case Type.LONG:
			case Type.DOUBLE:
				return false;
			case Type.ARRAY:
			case Type.OBJECT:
				if (type.equals(NULL_TYPE)) {
					return true;
				}
				// Convert 'type' to its element type and array dimension. Arrays of primitive values are
				// seen as Object arrays with one dimension less. Hence the element type is always of
				// Type.OBJECT sort.
				int dim = 0;
				if (type.getSort() == Type.ARRAY) {
					dim = type.getDimensions();
					type = type.getElementType();
					if (type.getSort() != Type.OBJECT) {
						dim = dim - 1;
						type = OBJECT_TYPE;
					}
				}
				// Do the same for expectedType.
				int expectedDim = 0;
				if (expectedType.getSort() == Type.ARRAY) {
					expectedDim = expectedType.getDimensions();
					expectedType = expectedType.getElementType();
					if (expectedType.getSort() != Type.OBJECT) {
						// If the expected type is an array of some primitive type, it does not have any subtype
						// other than itself. And 'type' is different by hypothesis.
						return false;
					}
				}
				// A type with less dimensions than expected can't be a subtype of the expected type.
				if (dim < expectedDim) {
					return false;
				}
				// A type with more dimensions than expected is seen as an array with the expected
				// dimensions but with an Object element type. For instance an array of arrays of Integer is
				// seen as an array of Object if the expected type is an array of Serializable.
				if (dim > expectedDim) {
					type = OBJECT_TYPE;
				}
				// type and expectedType have a Type.OBJECT sort by construction (see above),
				// as expected by isAssignableFrom.
				if (isAssignableFrom(expectedType, type)) {
					return true;
				}
				if (isInterface(expectedType)) {
					// The merge of class or interface types can only yield class types (because it is not
					// possible in general to find an unambiguous common super interface, due to multiple
					// inheritance). Because of this limitation, we need to relax the subtyping check here
					// if 'value' is an interface.
					return expectedType.equals(OBJECT_TYPE);
				} else {
					return false;
				}
			default:
				throw new AssertionError();
		}
	}

	@Override
	protected boolean isInterface(Type type) {
		if(type.getSort() != Type.OBJECT) return false;
		return tree.isInterface(type.getInternalName());
	}

	@Override
	protected Type getSuperClass(Type type) {
		if(type.getSort() != Type.OBJECT) return OBJECT_TYPE;
		return Type.getObjectType(tree.getParent(type.getInternalName(), 0));
	}

	@Override
	protected Class<?> getClass(Type type) {
		throw new UnsupportedOperationException("This verifier doesn't support loading classes");
	}
}
