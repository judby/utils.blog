package com.udby.blog.records.demo;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MutableTest {
    @Test
    void ctor_null_throwsNPE() {
        assertThatThrownBy(() -> new Mutable<>(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void ctor_wrongArrayLength_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new Mutable<>(new Object[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setIfNull_valueIsNull_setsNewValueAndReturnsTrue() {
        final var mutable = new Mutable<String>();

        final var changed = mutable.setIfNull("A");

        assertThat(changed).isTrue();
        assertThat(mutable.get()).isEqualTo("A");
    }

    @Test
    void setIfNull_valueIsNullSetToNull_returnsFalse() {
        final var mutable = new Mutable<String>();

        final var changed = mutable.setIfNull(null);

        assertThat(changed).isFalse();
        assertThat(mutable.get()).isNull();
    }

    @Test
    void setIfNull_valueIsNotNull_unchangedReturnsFalse() {
        final var mutable = Mutable.of("A");

        final var changed = mutable.setIfNull("B");

        assertThat(changed).isFalse();
        assertThat(mutable.get()).isEqualTo("A");
    }

    @Test
    void accept() {
        final var mutable = Mutable.of("A");

        mutable.accept("B");

        assertThat(mutable.isNull()).isFalse();
        assertThat(mutable.get()).isEqualTo("B");
    }

    @Test
    void set() {
        final var mutable = Mutable.of("A");

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
        final var mutable = Mutable.of(42);

        final var toString = mutable.toString();

        assertThat(toString).isEqualTo("42");
    }

    @Test
    void asOptional_null_empty() {
        final var mutable = new Mutable<>();

        final var optional = mutable.asOptional();

        assertThat(optional).isEmpty();
    }

    @Test
    void asOptional_notNull_optionalWithValue() {
        final var mutable = new Mutable<>("T");

        final var optional = mutable.asOptional();

        assertThat(optional).isNotEmpty()
                .get().isEqualTo("T");
    }

    @Test
    void stream_null_empty() {
        final var mutable = new Mutable<>();

        final var stream = mutable.stream();

        assertThat(stream).hasSize(0);
    }

    @Test
    void stream_notNull_streamWithValue() {
        final var mutable = new Mutable<>("T");

        final var stream = mutable.stream();

        assertThat(stream).hasSize(1)
                .contains("T");
    }

    @Test
    void ifPresent_null_noop() {
        final var mutable = new Mutable<>();

        final var called = new AtomicBoolean();

        mutable.ifPresent((x) -> called.set(true));

        assertThat(called).isFalse();
    }

    @Test
    void ifPresent_withValue_callsConsumer() {
        final var mutable = new Mutable<>("X");

        final var called = new AtomicReference<>();

        mutable.ifPresent(called::set);

        assertThat(called).hasValue("X");
    }

    @Test
    void ifPresentOrElse_null_callsEmptyAction() {
        final var mutable = new Mutable<>();

        final var consumerCalled = new AtomicReference<>();
        final var emptyActionCalled = new AtomicBoolean();

        mutable.ifPresentOrElse(consumerCalled::set, () -> emptyActionCalled.set(true));

        assertThat(consumerCalled).hasValue(null);
        assertThat(emptyActionCalled).isTrue();
    }

    @Test
    void ifPresentOrElse_withValue_callsConsumer() {
        final var mutable = new Mutable<>("X");

        final var consumerCalled = new AtomicReference<>();
        final var emptyActionCalled = new AtomicBoolean();

        mutable.ifPresentOrElse(consumerCalled::set, () -> emptyActionCalled.set(true));

        assertThat(consumerCalled).hasValue("X");
        assertThat(emptyActionCalled).isFalse();
    }

    @Test
    void filter_null_returnsEmpty() {
        final var mutable = new Mutable<>();

        final var optional = mutable.filter((x) -> false);

        assertThat(optional).isEmpty();
    }

    @Test
    void filter_withValueNoMatch_returnsEmpty() {
        final var mutable = new Mutable<>("X");

        final var optional = mutable.filter("Y"::equals);

        assertThat(optional).isEmpty();
    }

    @Test
    void filter_withValueMatch_returnsEmpty() {
        final var mutable = new Mutable<>("X");

        final var optional = mutable.filter("X"::equals);

        assertThat(optional).isNotEmpty()
                .get().isEqualTo("X");
    }

    @Test
    void orElse_null_returnsOther() {
        final var mutable = new Mutable<>();

        final var value = mutable.orElse("X");

        assertThat(value).isEqualTo("X");
    }

    @Test
    void orElse_withValue_returnsValue() {
        final var mutable = new Mutable<>("X");

        final var value = mutable.orElse("Y");

        assertThat(value).isEqualTo("X");
    }

    @Test
    void orElseGet_null_returnsOther() {
        final var mutable = new Mutable<>();

        final var value = mutable.orElseGet(() -> "X");

        assertThat(value).isEqualTo("X");
    }

    @Test
    void orElseGet_withValue_returnsValue() {
        final var mutable = new Mutable<>("X");

        final var value = mutable.orElseGet(() -> "Y");

        assertThat(value).isEqualTo("X");
    }

    @Test
    void orElseThrow_null_throws() {
        final var mutable = new Mutable<>();

        assertThatThrownBy(() -> mutable.orElseThrow(RuntimeException::new))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void orElseThrow_withValue_returnsValue() {
        final var mutable = new Mutable<>("X");

        final var value = mutable.orElseThrow(RuntimeException::new);

        assertThat(value).isEqualTo("X");
    }

    @Test
    void computeIfNull_null_obtainsAndReturnsNewValue() {
        final var mutable = new Mutable<String>();

        final var s = mutable.computeIfNull(() -> "X");

        assertThat(s).isEqualTo("X");
        assertThat(mutable.get()).isEqualTo("X");
    }

    @Test
    void computeIfNull_notNull_returnsCurrentValue() {
        final var mutable = new Mutable<>("X");

        final var s = mutable.computeIfNull(() -> "Y");

        assertThat(s).isEqualTo("X");
        assertThat(mutable.get()).isEqualTo("X");
    }

    @Test
    void value_testDefensiveCopy() {
        final var array = new String[]{"A"};

        final var mutable = new Mutable<String>(array);

        final var value = mutable.value();

        assertThat(value).isNotSameAs(array);
        assertThat(value[0]).isEqualTo("A");
    }
}
