package com.udby.blog.records.demo;

import java.time.InstantSource;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public record Timing(InstantSource timeSource, long started) {
    public Timing {
        Objects.requireNonNull(timeSource, "timeSource");
    }

    public Timing(InstantSource timeSource) {
        this(timeSource, timeSource.millis());
    }

    public static Timing start(InstantSource timeSource) {
        return new Timing(timeSource);
    }

    public static Timing start() {
        return start(InstantSource.system());
    }

    public long elapsedMs() {
        return elapsedMs(started);
    }

    private long elapsedMs(final long startMs) {
        return timeSource.millis() - startMs;
    }

    public <T, E extends Throwable> T timedOperation(Callable<T> operation, BiConsumer<Long, Exception> timingConsumer) throws E {
        final var startMs = timeSource.millis();
        final var caught = new AtomicReference<Exception>();
        try {
            return operation.call();
        } catch (Exception e) {
            caught.set(e);
            @SuppressWarnings("unchecked") final E toThrow = (E) e;
            throw toThrow;
        } finally {
            timingConsumer.accept(elapsedMs(startMs), caught.get());
        }
    }
}
