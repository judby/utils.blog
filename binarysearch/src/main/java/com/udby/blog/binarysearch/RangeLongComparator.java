package com.udby.blog.binarysearch;

public final class RangeLongComparator implements BiComparator<Range, Long> {
    @Override
    public int compare(Range range, Long aLong) {
        final var lowCompare = Long.compare(range.low(), aLong);
        if (lowCompare > 0) {
            return lowCompare;
        }
        final var highCompare = Long.compare(range.high(), aLong);
        if (highCompare <= 0) {
            return -1;
        }
        // aLong is in range [low, high)
        return 0;
    }
}
