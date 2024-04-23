package eu.bankopladerne.online.server.filecache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SimplestCache<K, V> {
    private final Map<K, V> cache;
    private final Semaphore sync;

    SimplestCache(Semaphore sync, int maxElements, Predicate<Map.Entry<K, V>> cleanPredicate, Consumer<Map.Entry<K, V>> cleaner) {
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                if (size() > maxElements || cleanPredicate.test(eldest)) {
                    cleaner.accept(eldest);
                    return true;
                }
                return false;
            }
        };
        this.sync = sync;
    }

    public SimplestCache(int maxElements, Predicate<Map.Entry<K, V>> cleanPredicate, Consumer<Map.Entry<K, V>> cleaner) {
        this(new Semaphore(1), maxElements, cleanPredicate, cleaner);
    }

    public SimplestCache(int maxElements) {
        this(maxElements, e -> false, e -> {
        });
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return acquireSemaphoreThen(() -> cache.computeIfAbsent(key, mappingFunction));
    }

    public V get(K key) {
        return acquireSemaphoreThen(() -> cache.get(key));
    }

    public V put(K key, V value) {
        return acquireSemaphoreThen(() -> cache.put(key, value));
    }

    public int size() {
        return cache.size();
    }

    private V acquireSemaphoreThen(Supplier<V> action) {
        try {
            sync.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        try {
            return action.get();
        } finally {
            sync.release();
        }
    }
}
