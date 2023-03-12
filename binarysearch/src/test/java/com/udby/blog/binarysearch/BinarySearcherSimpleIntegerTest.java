package com.udby.blog.binarysearch;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BinarySearcherSimpleIntegerTest {
    private final BinarySearcher<Integer> binarySearcher = BinarySearcher.ofElements(List.of(3, 1, 2));

    private final BiComparator<Integer, Integer> biComparator = Integer::compare;

    @Test
    void binarySearch_elementsFound_returnsIndex() {
        final var i1 = binarySearcher.binarySearch(1, biComparator);
        final var i2 = binarySearcher.binarySearch(2, biComparator);
        final var i3 = binarySearcher.binarySearch(3, biComparator);

        assertThat(i1).isEqualTo(0);
        assertThat(i2).isEqualTo(1);
        assertThat(i3).isEqualTo(2);
    }

    @Test
    void binarySearch_elementsNotFound_returnsInsertionIndex() {
        final var i1 = binarySearcher.binarySearch(0, biComparator);
        final var i2 = binarySearcher.binarySearch(4, biComparator);

        assertThat(i1).isEqualTo(-1);
        assertThat(i2).isEqualTo(-4);
    }
}
