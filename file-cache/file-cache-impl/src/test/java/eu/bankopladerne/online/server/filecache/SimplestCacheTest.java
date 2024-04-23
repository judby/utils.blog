package eu.bankopladerne.online.server.filecache;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class SimplestCacheTest {

    @Test
    void computeIfAbsent_didNotExist_invokesMappingFunction() {
        // Given
        final var simplestCache = new SimplestCache<String, String>(1);

        // When
        final var computed = simplestCache.computeIfAbsent("A", k -> "1");

        // Then
        assertThat(computed).isEqualTo("1");
    }

    @Test
    void computeIfAbsent_didExist_returnsExisting() {
        // Given
        final var simplestCache = new SimplestCache<String, String>(1);

        simplestCache.put("A", "1");

        // When
        final var computed = simplestCache.computeIfAbsent("A", k -> "x");

        // Then
        assertThat(computed).isEqualTo("1");
    }

    @Test
    void get_doesNotExist_returnsNull() {
        // Given
        final var simplestCache = new SimplestCache<String, String>(1);

        // When
        final var result = simplestCache.get("A");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void get_exists_returnsIt() {
        // Given
        final var simplestCache = new SimplestCache<String, String>(1);

        simplestCache.put("A", "1");

        // When
        final var result = simplestCache.get("A");

        // Then
        assertThat(result).isEqualTo("1");
    }

    @Test
    void put_tooMany_retainsLatestAccessed() {
        // Given
        final var simplestCache = new SimplestCache<String, String>(2);

        simplestCache.put("C", "3");
        simplestCache.put("A", "1");
        simplestCache.get("C");
        simplestCache.put("B", "2");

        // When
        final var first = simplestCache.get("A");
        final var last = simplestCache.get("B");
        final var accessed = simplestCache.get("C");

        // Then
        assertThat(first).isNull();
        assertThat(last).isEqualTo("2");
        assertThat(accessed).isEqualTo("3");
    }

    @Test
    void size() {
        // Given
        final var simplestCache = new SimplestCache<String, String>(2);

        IntStream.range(0, 10)
                .mapToObj(i -> "" + ('A' + i))
                .forEach(s -> simplestCache.put(s, s));

        // When - then
        assertThat(simplestCache.size()).isEqualTo(2);
    }

    @Test
    void ctor_withCleaner_succeeds() {
        // Given...
        // Flag for cleaner called
        final var cleaned = new AtomicBoolean();
        // We do not like "A"
        final var simplestCache = new SimplestCache<String, String>(2, e -> "A".equals(e.getKey()), e -> cleaned.set(true));

        // When
        simplestCache.put("A", "A");
        simplestCache.computeIfAbsent("B", k -> k);

        // Then
        assertThat(simplestCache.size()).isOne();
        assertThat(simplestCache.get("B")).isEqualTo("B");
        assertThat(cleaned).isTrue();
    }

    @Test
    void get_lockInterrupted_returnsNull() throws Exception {
        // Given
        final var semaphore = new Semaphore(1);

        final var simplestCache = new SimplestCache<String, String>(semaphore, 1, e -> false, e -> {
        });

        // A single element
        simplestCache.put("A", "A");

        // Hold the sempaphore (blocks get)
        semaphore.acquire();

        final var threadRunning = new CountDownLatch(1);
        final var get = new AtomicReference<String>();
        final var interrupted = new AtomicBoolean();

        final var thread = new Thread(() -> {
            threadRunning.countDown();

            // When
            final var s = simplestCache.get("A");

            get.set(s);
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        thread.start();

        // Wait until the thread is alive
        threadRunning.await();

        thread.interrupt();

        // Wait for it do be done
        thread.join();

        // Then
        assertThat(get.get()).isNull();
        assertThat(interrupted).isTrue();
    }
}
