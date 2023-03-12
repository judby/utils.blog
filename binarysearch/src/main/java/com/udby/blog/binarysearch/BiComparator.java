package com.udby.blog.binarysearch;

@FunctionalInterface
public interface BiComparator<T, K> {
    int compare(T element, K key);
}
