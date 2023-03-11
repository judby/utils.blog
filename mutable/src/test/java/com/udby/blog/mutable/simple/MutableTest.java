package com.udby.blog.mutable.simple;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class MutableTest {

    @Test
    void setIfNull_valueIsNull_setsNewValueAndReturnsTrue() {
        final var mutable = new Mutable<String>();

        final var changed = mutable.setIfNull("A");

        assertThat(changed).isTrue();
        assertThat(mutable.get()).isEqualTo("A");
    }

    @Test
    void setIfNull_valueIsNotNull_unchangedReturnsFalse() {
        final var mutable = new Mutable<>("A");

        final var changed = mutable.setIfNull("B");

        assertThat(changed).isFalse();
        assertThat(mutable.get()).isEqualTo("A");
    }

    @Test
    void accept() {
        final var mutable = new Mutable<>("A");

        mutable.accept("B");

        assertThat(mutable.isNull()).isFalse();
        assertThat(mutable.get()).isEqualTo("B");
    }

    @Test
    void set() {
        final var mutable = new Mutable<>("A");

        final var previousValue = mutable.set("B");

        assertThat(previousValue).isEqualTo("A");
        assertThat(mutable.get()).isEqualTo("B");
    }

    @Test
    void toString_valueIsNull_returnsStringNull() {
        final var mutable = new Mutable<>();

        final var toString = mutable.toString();

        assertThat(toString).isNotNull().isEqualTo("null");
    }

    @Test
    void toString_valueIsNotNull_returnsStringValueOf() {
        final var mutable = new Mutable<>(42);

        final var toString = mutable.toString();

        assertThat(toString).isEqualTo("42");
    }
}
