package com.udby.blog.mutable.simple;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class Mutable<T> implements Supplier<T>, Consumer<T> {
    private T value;

    public Mutable() {
    }

    public Mutable(T value) {
        this.value = value;
    }

    public boolean isNull() {
        return value == null;
    }

    public boolean setIfNull(T value) {
        if (isNull()) {
            set(value);
            return true;
        }
        return false;
    }

    public T set(T value) {
        final var old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public void accept(T value) {
        set(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
