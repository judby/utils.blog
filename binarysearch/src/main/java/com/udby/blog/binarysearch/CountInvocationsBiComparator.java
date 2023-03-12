package com.udby.blog.binarysearch;

import java.util.concurrent.atomic.AtomicInteger;

public record CountInvocationsBiComparator<T, K>(BiComparator<T, K> comparator, AtomicInteger invocations) implements BiComparator<T, K> {
    public CountInvocationsBiComparator(BiComparator<T, K> comparator) {
        this(comparator, new AtomicInteger());
    }

    public static <T, K> CountInvocationsBiComparator<T, K> from(BiComparator<T, K> comparator) {
        return new CountInvocationsBiComparator<>(comparator);
    }

    @Override
    public int compare(T t, K k) {
        invocations.incrementAndGet();
        return comparator.compare(t, k);
    }

    public int invocationsAndReset() {
        final var invocations = this.invocations.get();
        this.invocations.set(0);
        return invocations;
    }
}
