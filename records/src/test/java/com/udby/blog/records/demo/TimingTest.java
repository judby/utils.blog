package com.udby.blog.records.demo;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimingTest {
    @Test
    void ctor_nullTimeSource_throwsNPE() {
        assertThatThrownBy(() -> new Timing(null, 0))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("timeSource");
    }

    @Test
    void start_systemClock_succeeds() {
        final var now = Clock.systemDefaultZone().millis();
        final var timing = Timing.start();

        assertThat(timing.started()).isCloseTo(now, Offset.offset(10L));
    }

    @Test
    void elapsedMs() {
        final var T1 = 100L;
        final var DIFF = 20L;
        final var T2 = T1 + DIFF;
        // Using Mockito to mock the Clock to control "time"
        final var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(T1, T2);

        final var timing = Timing.start(clock);
        final var elapsedMs = timing.elapsedMs();

        assertThat(timing.timeSource()).isEqualTo(clock);
        assertThat(timing.started()).isEqualTo(T1);
        assertThat(elapsedMs).isEqualTo(DIFF);
    }

    @Test
    void timedOperation_success() {
        // Given
        final var T1 = 100L;
        final var DIFF = 20L;
        final var T2 = T1 + DIFF;
        // Using Mockito to mock the Clock to control "time"
        final var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(T1, T1, T2);

        final var reportedElapsed = new AtomicLong();
        BiConsumer<Long, Exception> biConsumer = (elapsed, exception) -> {
            reportedElapsed.set(elapsed);
            System.out.printf("Operation took %dms%n", elapsed);
        };

        final var timing = Timing.start(clock);

        // When
        final var value = timing.timedOperation(() -> "X", biConsumer);

        // Then
        assertThat(value).isEqualTo("X");
        assertThat(reportedElapsed.get()).isEqualTo(DIFF);
    }

    @Test
    void timedOperation_exception() {
        // Given
        final var T1 = 100L;
        final var DIFF = 20L;
        final var T2 = T1 + DIFF;
        // Using Mockito to mock the Clock to control "time"
        final var clock = mock(Clock.class);
        when(clock.millis()).thenReturn(T1, T1, T2);

        final var thrown = new Exception("FAILED");
        final var reportedElapsed = new AtomicLong();
        BiConsumer<Long, Exception> biConsumer = (elapsed, exception) -> {
            reportedElapsed.set(elapsed);
            if (exception != null) {
                System.out.printf("Operation %s took %dms%n", exception, elapsed);
            }
        };

        final var timing = Timing.start(clock);

        // When
        assertThatThrownBy(() -> timing.timedOperation(() -> {
            throw thrown;
        }, biConsumer))
                .isEqualTo(thrown);

        // Then
        assertThat(reportedElapsed.get()).isEqualTo(DIFF);
    }
}
