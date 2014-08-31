package com.bumptech.glide.util;

/**
 * A key of two {@link Class}es to be used in hashed collections.
 */
public class MultiClassKey {
    private Class<?> first;
    private Class<?> second;

    public MultiClassKey() {
        // leave them null
    }

    public MultiClassKey(Class<?> first, Class<?> second) {
        set(first, second);
    }

    public void set(Class<?> first, Class<?> second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public String toString() {
        return "MultiClassKey{"
                + "first=" + first
                + ", second=" + second
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        MultiClassKey that = (MultiClassKey) o;

        if (!first.equals(that.first)) {
            return false;
        }
        if (!second.equals(that.second)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = first.hashCode();
        result = 31 * result + second.hashCode();
        return result;
    }
}
