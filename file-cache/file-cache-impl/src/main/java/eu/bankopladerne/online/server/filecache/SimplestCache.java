/*
 * Copyright 2024 Jesper Udby
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    /**
     * CTOR exposing internals for testing purposes only...
     */
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

    /**
     * Create simple thread-safe LRU cache containing at most maxElements elements or whatever the cleanPredicate dictates. Also
     * supports a cleaning operation which is invoked when old elements are evicted from the cache. This is called
     * while a lock is held on the cache so should be short/fast and not lock on other elements.
     *
     * @param maxElements    Max number elements to cache
     * @param cleanPredicate Alternative predicate - return true if oldest element is to be evicted even if maxElements
     *                       is not reached
     * @param cleaner        Reference to a cleaning method doing cleanup of external resources when oldest element is
     *                       evicted.
     */
    public SimplestCache(int maxElements, Predicate<Map.Entry<K, V>> cleanPredicate, Consumer<Map.Entry<K, V>> cleaner) {
        this(new Semaphore(1), maxElements, cleanPredicate, cleaner);
    }

    /**
     * Create a simple thread-safe LRU cache containing at most maxElements elements.
     *
     * @param maxElements Max number elements to cache
     */
    public SimplestCache(int maxElements) {
        this(maxElements, e -> false, e -> {
        });
    }

    /**
     * Compute and cache V if not already cached, returns V if cached.
     *
     * @param key             Key
     * @param mappingFunction Function to compute V
     * @return Cached value V for key
     * @see Map#computeIfAbsent(Object, Function)
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return acquireSemaphoreThen(() -> cache.computeIfAbsent(key, mappingFunction));
    }

    /**
     * Returns V if cached, null if not
     *
     * @param key Key
     * @return Cached value V for key
     * @see Map#get(Object)
     */
    public V get(K key) {
        return acquireSemaphoreThen(() -> cache.get(key));
    }

    /**
     * Cache a value V for the Key given, returns previously set value or null if not
     *
     * @param key   Key
     * @param value Value V to set for key
     * @return Previous value or null
     * @see Map#put(Object, Object)
     */
    public V put(K key, V value) {
        return acquireSemaphoreThen(() -> cache.put(key, value));
    }

    /**
     * Returns number of elements currently cached
     *
     * @return Number of elements in the cache
     */
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
