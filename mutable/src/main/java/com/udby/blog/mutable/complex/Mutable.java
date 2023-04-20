package com.udby.blog.mutable.complex;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class Mutable<T> implements Supplier<T>, Consumer<T> {
    private T value;

    public static <T> Mutable<T> of(T value) {
        return new Mutable<>(value);
    }

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
            return value != null;
        }
        return false;
    }

    public T set(T value) {
        final var old = this.value;
        this.value = value;
        return old;
    }

    public Optional<T> asOptional() {
        return Optional.ofNullable(value);
    }

    public Stream<T> stream() {
        if (isNull()) {
            return Stream.empty();
        } else {
            return Stream.of(value);
        }
    }

    public void ifPresent(Consumer<? super T> action) {
        if (!isNull()) {
            action.accept(value);
        }
    }

    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction) {
        if (isNull()) {
            emptyAction.run();
        } else {
            action.accept(value);
        }
    }

    public Optional<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        if (isNull()) {
            return Optional.empty();
        } else {
            return predicate.test(value) ? Optional.of(value) : Optional.empty();
        }
    }

    public T orElse(T other) {
        return isNull() ? other : value;
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        return isNull() ? supplier.get() : value;
    }

    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isNull()) {
            throw exceptionSupplier.get();
        } else {
            return value;
        }
    }

    public T computeIfNull(Supplier<T> supplier) {
        if (isNull()) {
            set(supplier.get());
        }
        return this.value;
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
