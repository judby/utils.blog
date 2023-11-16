package com.udby.blog.records.demo;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record Mutable<T>(T[] value) implements Supplier<T>, Consumer<T> {

    public static <T> Mutable<T> of(T value) {
        return new Mutable<>(value);
    }

    public Mutable {
        if (Objects.requireNonNull(value).length != 1) {
            throw new IllegalArgumentException("Array must have length 1");
        }
    }

    public Mutable() {
        this(arrayWithValue(null));
    }

    public Mutable(T value) {
        this(arrayWithValue(value));
    }

    private static <T> T[] arrayWithValue(T value) {
        T[] array = (T[]) new Object[1];
        array[0] = value;
        return array;
    }

    public boolean isNull() {
        return get() == null;
    }

    public boolean setIfNull(T value) {
        if (isNull()) {
            set(value);
            return value != null;
        }
        return false;
    }

    public T set(T value) {
        final var old = this.value[0];
        this.value[0] = value;
        return old;
    }

    public Optional<T> asOptional() {
        return Optional.ofNullable(get());
    }

    public Stream<T> stream() {
        if (isNull()) {
            return Stream.empty();
        } else {
            return Stream.of(get());
        }
    }

    public void ifPresent(Consumer<? super T> action) {
        if (!isNull()) {
            action.accept(get());
        }
    }

    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (isNull()) {
            emptyAction.run();
        } else {
            action.accept(get());
        }
    }

    public Optional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (isNull()) {
            return Optional.empty();
        } else {
            return predicate.test(get()) ? Optional.of(get()) : Optional.empty();
        }
    }

    public T orElse(T other) {
        return isNull() ? other : get();
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        return isNull() ? supplier.get() : get();
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isNull()) {
            throw exceptionSupplier.get();
        } else {
            return get();
        }
    }

    public T computeIfNull(Supplier<T> supplier) {
        if (isNull()) {
            set(supplier.get());
        }
        return get();
    }

    @Override
    public T get() {
        return value[0];
    }

    @Override
    public void accept(T value) {
        set(value);
    }

    @Override
    public String toString() {
        return String.valueOf(get());
    }

    @Override
    public T[] value() {
        // return defensive copy
        return Arrays.copyOf(value, 1);
    }
}
