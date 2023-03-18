package com.udby.blog.binarysearch;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class BinarySearcherRangeStatisticsTest {
    private final BinarySearcher<Range> binarySearcher;

    {
        final var elements = new ArrayList<Range>();
        for (long i = 0; i < 1_000_000L; i++) {
            elements.add(new Range("[%d..%d)".formatted(i * 100L, i * 100L + 100L), i * 100L, i * 100L + 100L));
        }
        binarySearcher = new BinarySearcher<>(elements);
    }

    @Test
    void test() {
        final var comparator = CountInvocationsBiComparator.from(new RangeLongComparator());

        final var index = binarySearcher.binarySearch(1L, comparator);

        assertThat(index).isEqualTo(0);

        System.out.printf("%d comparisons%n", comparator.invocationsAndReset());
    }
}
