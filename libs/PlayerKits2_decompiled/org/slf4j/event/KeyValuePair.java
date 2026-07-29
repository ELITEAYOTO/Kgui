/*
 * Decompiled with CFR 0.152.
 */
package org.slf4j.event;

import java.util.Objects;

public class KeyValuePair {
    public final String key;
    public final Object value;

    public KeyValuePair(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    public String toString() {
        return String.valueOf(this.key) + "=\"" + String.valueOf(this.value) + "\"";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        KeyValuePair that = (KeyValuePair)o;
        return Objects.equals(this.key, that.key) && Objects.equals(this.value, that.value);
    }

    public int hashCode() {
        return Objects.hash(this.key, this.value);
    }
}

