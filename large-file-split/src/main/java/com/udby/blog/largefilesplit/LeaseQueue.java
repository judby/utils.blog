package com.udby.blog.largefilesplit;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class LeaseQueue<T> {
    private final Queue<T> leaseQueue;
    private final Supplier<T> newInstance;
    private final Consumer<T> resetInstance;

    public LeaseQueue(int maxSize, Supplier<T> newInstance, Consumer<T> resetInstance) {
        this.leaseQueue = new ArrayBlockingQueue<>(maxSize);
        this.newInstance = newInstance;
        this.resetInstance = resetInstance;
    }

    public void offer(T value) {
        leaseQueue.offer(value);
    }

    public Lease lease() {
        return new Lease();
    }

    public <R> R leaseAndDo(Function<T, R> action) {
        try (final var lease = lease()) {
            return action.apply(lease.get());
        }
    }

    private T newInstanceIfQueueIsEmpty() {
        final T value = leaseQueue.poll();
        return value == null ? newInstance.get() : value;
    }

    public class Lease implements AutoCloseable, Supplier<T> {
        private final T value;
        private volatile boolean closed;

        private Lease() {
            this.value = newInstanceIfQueueIsEmpty();
        }

        @Override
        public void close() {
            if (!closed) {
                this.closed = true;
                resetInstance.accept(this.value);
                leaseQueue.offer(this.value);
            }
        }

        @Override
        public T get() {
            if (closed) {
                throw new IllegalStateException("Is closed");
            }
            return value;
        }
    }
}
