/*
 * Copyright (c) 2009-2011 Ken Wenzel and Mathias Doenitz
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.parboiled.transform;

import com.google.common.base.Preconditions;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.parboiled.BaseParser;
import org.parboiled.ContextAware;
import org.parboiled.support.Var;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class AsmUtils
{
    private AsmUtils()
    {
    }

    public static String getExtendedParserClassName(
        final String parserClassName)
    {
        Preconditions.checkNotNull(parserClassName, "parserClassName");
        return parserClassName + "$$parboiled";
    }

    private static final Map<String, Class<?>> classForDesc
        = new HashMap<String, Class<?>>();

    public static synchronized Class<?> getClassForInternalName(
        final String classDesc)
    {
        Preconditions.checkNotNull(classDesc, "classDesc");
        Class<?> clazz = classForDesc.get(classDesc);
        if (clazz != null)
            return clazz;

        if (classDesc.charAt(0) == '[') {
            final Class<?> compType = getClassForType(
                Type.getType(classDesc.substring(1)));
            clazz = Array.newInstance(compType, 0).getClass();
        } else {
            final String className = classDesc.replace('/', '.');
            try {
                clazz = AsmUtils.class.getClassLoader().loadClass(className);
            } catch (ClassNotFoundException e) {
                // If class not found trying the context classLoader
                try {
                    clazz = Thread.currentThread().getContextClassLoader()
                        .loadClass(className);
                } catch (ClassNotFoundException e2) {
                    throw new RuntimeException(
                        "Error loading class '" + className
                            + "' for rule method analysis", e2);
                }
            }
        }
        classForDesc.put(classDesc, clazz);
        return clazz;
    }

    public static Class<?> getClassForType(final Type type)
    {
        Preconditions.checkNotNull(type, "type");
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return boolean.class;
            case Type.BYTE:
                return byte.class;
            case Type.CHAR:
                return char.class;
            case Type.DOUBLE:
                return double.class;
            case Type.FLOAT:
                return float.class;
            case Type.INT:
                return int.class;
            case Type.LONG:
                return long.class;
            case Type.SHORT:
                return short.class;
            case Type.VOID:
                return void.class;
            case Type.OBJECT:
            case Type.ARRAY:
                return getClassForInternalName(type.getInternalName());
        }
        throw new IllegalStateException(); // should be unreachable
    }

    public static Field getClassField(final String classInternalName,
        final String fieldName)
    {
        Preconditions.checkNotNull(classInternalName, "classInternalName");
        Preconditions.checkNotNull(fieldName, "fieldName");
        final Class<?> clazz = getClassForInternalName(classInternalName);
        Class<?> current = clazz;
        while (true) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
                if (Object.class.equals(current)) {
                    throw new RuntimeException(
                        "Field '" + fieldName + "' not found in '" + clazz
                            + "\' or any superclass", e);
                }
            }
        }
    }

    public static Method getClassMethod(final String classInternalName,
        final String methodName, final String methodDesc)
    {
        Preconditions.checkNotNull(classInternalName, "classInternalName");
        Preconditions.checkNotNull(methodName, "methodName");
        Preconditions.checkNotNull(methodDesc, "methodDesc");
        final Class<?> clazz = getClassForInternalName(classInternalName);
        final Type[] types = Type.getArgumentTypes(methodDesc);
        final Class<?>[] argTypes = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            argTypes[i] = getClassForType(types[i]);
        }
        final Method method = findMethod(clazz, methodName, argTypes);
        if (method == null) {
            throw new RuntimeException(
                "Method '" + methodName + "' with descriptor '" +
                    methodDesc + "' not found in '" + clazz
                    + "\' or any supertype");
        }
        return method;
    }

    private static Method findMethod(final Class<?> clazz,
        final String methodName, final Class<?>[] argTypes)
    {
        Method found = null;
        if (clazz != null) {
            try {
                found = clazz.getDeclaredMethod(methodName, argTypes);
            } catch (NoSuchMethodException e) {
                found = findMethod(clazz.getSuperclass(), methodName, argTypes);
                if (found == null) {
                    for (final Class<?> interfaceClass : clazz
                        .getInterfaces()) {
                        found = findMethod(interfaceClass, methodName,
                            argTypes);
                        if (found != null)
                            break;
                    }
                }
            }
        }
        return found;
    }

    public static Constructor<?> getClassConstructor(
        final String classInternalName, final String constructorDesc)
    {
        Preconditions.checkNotNull(classInternalName, "classInternalName");
        Preconditions.checkNotNull(constructorDesc, "constructorDesc");
        final Class<?> clazz = getClassForInternalName(classInternalName);
        final Type[] types = Type.getArgumentTypes(constructorDesc);
        final Class<?>[] argTypes = new Class<?>[types.length];
        for (int i = 0; i < types.length; i++) {
            argTypes[i] = getClassForType(types[i]);
        }
        try {
            return clazz.getDeclaredConstructor(argTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                "Constructor with descriptor '" + constructorDesc
                    + "' not found in '" +
                    clazz, e);
        }
    }

    /**
     * Returns the class with the given name if it has already been loaded by the given class loader.
     * Otherwise the method returns null.
     *
     * @param className the full name of the class to be loaded
     * @param classLoader the class loader to use
     * @return the class instance or null
     */
    public static Class<?> findLoadedClass(final String className,
        final ClassLoader classLoader)
    {
        Preconditions.checkNotNull(className, "className");
        Preconditions.checkNotNull(classLoader, "classLoader");
        try {
            final Class<?> classLoaderBaseClass = Class
                .forName("java.lang.ClassLoader");
            final Method findLoadedClassMethod = classLoaderBaseClass
                .getDeclaredMethod("findLoadedClass", String.class);

            // protected method invocation
            findLoadedClassMethod.setAccessible(true);
            try {
                return (Class<?>) findLoadedClassMethod
                    .invoke(classLoader, className);
            } finally {
                findLoadedClassMethod.setAccessible(false);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Could not determine whether class '" + className +
                    "' has already been loaded", e);
        }
    }

    /**
     * Loads the class defined with the given name and bytecode using the given class loader.
     * Since package and class idendity includes the ClassLoader instance used to load a class we use reflection
     * on the given class loader to define generated classes. If we used our own class loader (in order to be able
     * to access the protected "defineClass" method) we would likely still be able to load generated classes,
     * however, they would not have access to package-private classes and members of their super classes.
     *
     * @param className the full name of the class to be loaded
     * @param code the bytecode of the class to load
     * @param classLoader the class loader to use
     * @return the class instance
     */
    public static Class<?> loadClass(final String className, final byte[] code,
        final ClassLoader classLoader)
    {
        Preconditions.checkNotNull(className, "className");
        Preconditions.checkNotNull(code, "code");
        Preconditions.checkNotNull(classLoader, "classLoader");
        try {
            final Class<?> classLoaderBaseClass = Class
                .forName("java.lang.ClassLoader");
            final Method defineClassMethod = classLoaderBaseClass
                .getDeclaredMethod("defineClass", String.class, byte[].class,
                    int.class, int.class);

            // protected method invocation
            defineClassMethod.setAccessible(true);
            try {
                return (Class<?>) defineClassMethod
                    .invoke(classLoader, className, code, 0, code.length);
            } finally {
                defineClassMethod.setAccessible(false);
            }
        } catch (Exception e) {
            throw new RuntimeException(
                "Could not load class '" + className + '\'', e);
        }
    }

    public static InsnList createArgumentLoaders(final String methodDescriptor)
    {
        Preconditions.checkNotNull(methodDescriptor, "methodDescriptor");
        final InsnList instructions = new InsnList();
        final Type[] types = Type.getArgumentTypes(methodDescriptor);
        for (int i = 0; i < types.length; i++) {
            instructions
                .add(new VarInsnNode(getLoadingOpcode(types[i]), i + 1));
        }
        return instructions;
    }

    public static int getLoadingOpcode(final Type argType)
    {
        Preconditions.checkNotNull(argType, "argType");
        switch (argType.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.ILOAD;
            case Type.DOUBLE:
                return Opcodes.DLOAD;
            case Type.FLOAT:
                return Opcodes.FLOAD;
            case Type.LONG:
                return Opcodes.LLOAD;
            case Type.OBJECT:
            case Type.ARRAY:
                return Opcodes.ALOAD;
            default:
                throw new IllegalStateException();
        }
    }

    /**
     * Determines whether the class with the given descriptor is assignable to the given type.
     *
     * @param classInternalName the class descriptor
     * @param type the type
     * @return true if the class with the given descriptor is assignable to the given type
     */
    public static boolean isAssignableTo(final String classInternalName,
        final Class<?> type)
    {
        Preconditions.checkNotNull(classInternalName, "classInternalName");
        Preconditions.checkNotNull(type, "type");
        return type
            .isAssignableFrom(getClassForInternalName(classInternalName));
    }

    public static boolean isBooleanValueOfZ(final AbstractInsnNode insn)
    {
        Preconditions.checkNotNull(insn, "insn");
        if (insn.getOpcode() != Opcodes.INVOKESTATIC)
            return false;
        final MethodInsnNode mi = (MethodInsnNode) insn;
        return isBooleanValueOfZ(mi.owner, mi.name, mi.desc);
    }

    public static boolean isBooleanValueOfZ(final String methodOwner,
        final String methodName, final String methodDesc)
    {
        Preconditions.checkNotNull(methodOwner, "methodOwner");
        Preconditions.checkNotNull(methodName, "methodName");
        Preconditions.checkNotNull(methodDesc, "methodDesc");
        return "java/lang/Boolean".equals(methodOwner) && "valueOf"
            .equals(methodName) &&
            "(Z)Ljava/lang/Boolean;".equals(methodDesc);
    }

    public static boolean isActionRoot(final AbstractInsnNode insn)
    {
        Preconditions.checkNotNull(insn, "insn");
        if (insn.getOpcode() != Opcodes.INVOKESTATIC)
            return false;
        final MethodInsnNode mi = (MethodInsnNode) insn;
        return isActionRoot(mi.owner, mi.name);
    }

    public static boolean isActionRoot(final String methodOwner,
        final String methodName)
    {
        Preconditions.checkNotNull(methodOwner, "methodOwner");
        Preconditions.checkNotNull(methodName, "methodName");
        return "ACTION".equals(methodName) && isAssignableTo(methodOwner,
            BaseParser.class);
    }

    public static boolean isVarRoot(final AbstractInsnNode insn)
    {
        Preconditions.checkNotNull(insn, "insn");
        if (insn.getOpcode() != Opcodes.INVOKESPECIAL)
            return false;
        final MethodInsnNode mi = (MethodInsnNode) insn;
        return isVarRoot(mi.owner, mi.name, mi.desc);
    }

    public static boolean isVarRoot(final String methodOwner,
        final String methodName, final String methodDesc)
    {
        Preconditions.checkNotNull(methodOwner, "methodOwner");
        Preconditions.checkNotNull(methodName, "methodName");
        Preconditions.checkNotNull(methodDesc, "methodDesc");
        return "<init>".equals(methodName) && "(Ljava/lang/Object;)V"
            .equals(methodDesc) &&
            isAssignableTo(methodOwner, Var.class);
    }

    public static boolean isCallOnContextAware(final AbstractInsnNode insn)
    {
        Preconditions.checkNotNull(insn, "insn");
        if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL
            && insn.getOpcode() != Opcodes.INVOKEINTERFACE)
            return false;
        final MethodInsnNode mi = (MethodInsnNode) insn;
        return isAssignableTo(mi.owner, ContextAware.class);
    }
}
