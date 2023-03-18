package com.udby.blog.binarysearch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BinarySearcherRangeSimpleTest {
    private final BinarySearcher<Range> rangeBinarySearcher = new BinarySearcher<>(List.of(
            new Range("[10,20)", 10, 20),
            new Range("[20,30)", 20, 30),
            new Range("[30,40)", 30, 40)));

    public static Stream<Arguments> rangesInput() {
        return Stream.of(
                Arguments.of(1, -1),
                Arguments.of(10, 0),
                Arguments.of(19, 0),
                Arguments.of(20, 1),
                Arguments.of(29, 1),
                Arguments.of(30, 2),
                Arguments.of(39, 2),
                Arguments.of(40, -4)
        );
    }

    @ParameterizedTest
    @MethodSource("rangesInput")
    void testBinarySearch(long input, int expectedIndex) {
        final var rangeLongComparator = new RangeLongComparator();

        final var actualIndex = rangeBinarySearcher.binarySearch(input, rangeLongComparator);

        assertThat(actualIndex).isEqualTo(expectedIndex);
    }

    @Test
    void find_elementFound_returnsOptionalWithElement() {
        final var range = rangeBinarySearcher.find(15L, new RangeLongComparator());

        assertThat(range).isNotEmpty().get().extracting("id").isEqualTo("[10,20)");
    }

    @Test
    void find_elementNotFound_returnsEmptyOptional() {
        final var range = rangeBinarySearcher.find(40L, new RangeLongComparator());

        assertThat(range).isEmpty();
    }

    @Test
    void elements_immutable() {
        assertThatThrownBy(() -> rangeBinarySearcher.elements().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
