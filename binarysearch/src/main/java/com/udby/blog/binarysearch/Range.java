package com.udby.blog.binarysearch;

import java.util.Comparator;

public record Range(String id, long low, long high) implements Comparable<Range> {
    private static final Comparator<Range> COMPARATOR = Comparator.comparing(Range::low).thenComparing(Range::high);

    @Override
    public int compareTo(Range o) {
        return COMPARATOR.compare(this, o);
    }
}
