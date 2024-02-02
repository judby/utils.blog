package com.udby.blog.records.demo;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;

/**
 * Simple DateRange of LocalDate's both dates inclusive: [from, to]
 *
 * @param from
 * @param to
 */
public record DateRange(LocalDate from, LocalDate to) implements Comparable<DateRange> {
    private static Pattern SUPPORTED_PATTERNS =
            Pattern.compile("^" +
                    "(\\d{4}-\\d{2})|" +
                    "(\\d{4}-\\d{2}-\\d{2})|" +
                    "(\\d{4}-\\d{2}-\\d{2}),(\\d{4}-\\d{2}-\\d{2})|" +
                    "[\\[\\(](\\d{4}-\\d{2}-\\d{2}),(\\d{4}-\\d{2}-\\d{2})[\\]\\)]" +
                    "$");

    public static final Comparator<DateRange> COMPARATOR = Comparator.comparing(DateRange::from).thenComparing(DateRange::to);

    public DateRange {
        if (Objects.requireNonNull(from, "from").isAfter(Objects.requireNonNull(to, "to"))) {
            throw new IllegalArgumentException("from %s cannot be after %s".formatted(from, to));
        }
    }

    public static DateRange parse(String dateRangeAsString) {
        final var matcher = SUPPORTED_PATTERNS.matcher(Objects.requireNonNull(dateRangeAsString));
        if (matcher.matches()) {
            if (matcher.group(1) != null) {
                return ofMonth(LocalDate.parse(matcher.group(1) + "-01"));
            } else if (matcher.group(2) != null) {
                final var theDate = LocalDate.parse(matcher.group(2));
                return new DateRange(theDate, theDate);
            } else if (matcher.group(3) != null && matcher.group(4) != null) {
                final var from = LocalDate.parse(matcher.group(3));
                final var to = LocalDate.parse(matcher.group(4));

                return new DateRange(from, to);
            } else if (matcher.group(5) != null && matcher.group(6) != null) {
                final var fromAdjust = dateRangeAsString.charAt(0) == '(';
                final var toAdjust = dateRangeAsString.charAt(dateRangeAsString.length() - 1) == ')';

                final var from = fromAdjust ? LocalDate.parse(matcher.group(5)).plusDays(1) : LocalDate.parse(matcher.group(5));
                final var to = toAdjust ? LocalDate.parse(matcher.group(6)).minusDays(1) : LocalDate.parse(matcher.group(6));

                return new DateRange(from, to);
            }
        }
        throw new IllegalArgumentException("%s is not a supported DateRange pattern".formatted(dateRangeAsString));
    }

    public static DateRange fromPreviousMonth(LocalDate date) {
        final var firstInMonth = date.withDayOfMonth(1);
        final var to = firstInMonth.minusDays(1);
        final var from = to.withDayOfMonth(1);
        return new DateRange(from, to);
    }

    public static DateRange ofMonth(LocalDate dayInMonth) {
        final var from = dayInMonth.withDayOfMonth(1);
        final var to = dayInMonth.withDayOfMonth(dayInMonth.lengthOfMonth());
        return new DateRange(from, to);
    }

    public boolean sameDate() {
        return from.equals(to);
    }

    public int daysInRange() {
        return (int) (DAYS.between(from, to) + 1);
    }

    @Override
    public int compareTo(DateRange o) {
        return COMPARATOR.compare(this, o);
    }

    public int compare(LocalDate o) {
        final var date = Objects.requireNonNull(o, "date");
        final var lowCompare = from.compareTo(date);
        if (lowCompare > 0) {
            return lowCompare;
        }
        final var highCompare = to.compareTo(date);
        if (highCompare < 0) {
            return -1;
        }
        // date is in range [from,to]
        return 0;
    }

    @Override
    public String toString() {
        return sameDate() ? "[%s]".formatted(from) : "[%s,%s]".formatted(from, to);
    }

    /**
     * Between dates operating as the SQL variant, both ends inclusive
     *
     * @param test LocalDate to test
     * @return true of the date is in the range [from,to]
     */
    public boolean between(LocalDate test) {
        return compare(test) == 0;
    }

    public Stream<LocalDate> dates() {
        long end = to.toEpochDay() + 1;
        long start = from.toEpochDay();
        return LongStream.range(start, end).mapToObj(LocalDate::ofEpochDay);
    }
}
