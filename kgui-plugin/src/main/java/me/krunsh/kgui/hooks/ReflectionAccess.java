package me.krunsh.kgui.hooks;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Utilitaires internes pour isoler les classloaders des soft-dependencies. */
final class ReflectionAccess {
    private ReflectionAccess() {
    }

    static Class<?> load(ClassLoader loader, String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }

    static Method method(Class<?> type, String name, int parameters) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterTypes().length == parameters) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + '#' + name + '/' + parameters);
    }

    static Method compatibleMethod(Class<?> type, String name, Object... arguments) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterTypes().length != arguments.length) continue;
            Class<?>[] types = method.getParameterTypes();
            boolean compatible = true;
            for (int index = 0; index < types.length; index++) {
                if (!compatible(types[index], arguments[index])) {
                    compatible = false;
                    break;
                }
            }
            if (compatible) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new NoSuchMethodException(type.getName() + '#' + name + '/' + arguments.length);
    }

    static Object construct(Class<?> type) throws ReflectiveOperationException {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    static Object invoke(Object target, Method method, Object... arguments) throws ReflectiveOperationException {
        return method.invoke(target, arguments);
    }

    private static boolean compatible(Class<?> expected, Object value) {
        if (value == null) return !expected.isPrimitive();
        if (!expected.isPrimitive()) return expected.isInstance(value);
        if (expected == boolean.class) return value instanceof Boolean;
        if (expected == char.class) return value instanceof Character;
        return value instanceof Number;
    }
}
