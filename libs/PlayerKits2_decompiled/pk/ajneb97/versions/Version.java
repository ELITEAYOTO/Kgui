/*
 * Decompiled with CFR 0.152.
 */
package pk.ajneb97.versions;

import java.lang.reflect.Method;
import java.util.HashMap;

public class Version {
    private HashMap<String, Class<?>> classes = new HashMap();
    private HashMap<String, Method> methods = new HashMap();

    public void addClass(String name, Class<?> classType) {
        this.classes.put(name, classType);
    }

    public void addMethod(String name, Method methodType) {
        this.methods.put(name, methodType);
    }

    public Class<?> getClassRef(String name) {
        return this.classes.get(name);
    }

    public Method getMethodRef(String name) {
        return this.methods.get(name);
    }
}

