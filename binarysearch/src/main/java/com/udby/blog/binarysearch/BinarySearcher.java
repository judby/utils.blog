package com.udby.blog.binarysearch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A BinarySearcher able to do binary searches in list of elements of type T.
 * If the constructor is used the list of elements supplied must be sorted,
 * or use the simple factory method ofElements()
 *
 * @param elements
 * @param <T>
 */
public record BinarySearcher<T>(List<T> elements) {
    public static <T> BinarySearcher<T> ofElements(Collection<T> elements) {
        // Make defensive copy
        final var list = new ArrayList<T>(elements);
        // Sort it according to natural ordering
        list.sort(null);
        return new BinarySearcher<>(list);
    }

    /**
     * Searches the list for the specified key using the binary search algorithm.
     *
     * @param key          the key to be searched for
     * @param biComparator the comparator used in the binary search algorithm
     * @param <K>          Type of key to lookup
     * @return the index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1).
     * The insertion point is defined as the point at which the key would be inserted into the list: the index of the
     * first element greater than the key, or list.size() if all elements in the list are less than the specified key.
     * Note that this guarantees that the return value will be >= 0 if and only if the key is found.
     */
    public <K> int binarySearch(final K key, final BiComparator<T, K> biComparator) {
        return binarySearch(elements, key, biComparator);
    }

    public <K> Optional<T> find(final K key, final BiComparator<T, K> biComparator) {
        final var index = binarySearch(elements, key, biComparator);
        return index < 0 ? Optional.empty() : Optional.of(elements.get(index));
    }

    public static <T, K> int binarySearch(final List<T> elements, final K key, final BiComparator<T, K> biComparator) {
        var low = 0;
        var high = elements.size() - 1;

        while (low <= high) {
            final var mid = (low + high) >>> 1;
            final var element = elements.get(mid);
            final var cmp = biComparator.compare(element, key);

            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return mid; // key found
            }
        }
        return -(low + 1);  // key not found
    }

    @Override
    public List<T> elements() {
        // Immutable - list is owned by BinarySearcher
        return Collections.unmodifiableList(elements);
    }
}
