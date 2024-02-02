package com.udby.blog.records.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {
    @Test
    void ctor_fromNull_throwsNPE() {
        assertThatThrownBy(() -> new DateRange(null, LocalDate.MAX))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("from");
    }

    @Test
    void ctor_toNull_throwsNPE() {
        assertThatThrownBy(() -> new DateRange(LocalDate.MIN, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("to");
    }

    @Test
    void ctor_fromAfterTo_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-16")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2023-03-16")
                .hasMessageContaining("2023-03-17");
    }

    @Test
    void sameDate_sameFromAndTo_returnsTrue() {
        final var dateRange = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-17"));

        assertThat(dateRange.sameDate()).isTrue();
    }

    @Test
    void sameDate_differentFromAndTo_returnsFalse() {
        final var dateRange = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-18"));

        assertThat(dateRange.sameDate()).isFalse();
    }

    @Test
    void daysInRange_sameFromAndTo_returnsOne() {
        final var dateRange = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-17"));

        assertThat(dateRange.daysInRange()).isOne();
    }

    @Test
    void daysInRange_firstAndLastDateInFeb2023_returnsDaysBetween() {
        final var dateRange = new DateRange(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28"));

        assertThat(dateRange.daysInRange()).isEqualTo(28);
    }

    @Test
    void compareTo_same_returnsZero() {
        final var dateRange1 = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-17"));
        final var dateRange2 = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-17"));

        assertThat(dateRange1.compareTo(dateRange2)).isZero();
        assertThat(dateRange2.compareTo(dateRange1)).isZero();
        assertThat(dateRange1.compareTo(dateRange1)).isZero();
    }

    @Test
    void compareTo_fromBeforeTo_returnsMinus() {
        final var dateRange1 = new DateRange(LocalDate.parse("2023-03-18"), LocalDate.parse("2023-03-18"));
        final var dateRange2 = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-18"));
        final var dateRange3 = new DateRange(LocalDate.parse("2023-03-18"), LocalDate.parse("2023-03-19"));

        assertThat(dateRange2.compareTo(dateRange1)).isNegative();
        assertThat(dateRange2.compareTo(dateRange3)).isNegative();
    }

    @Test
    void compareTo_fromAfterTo_returnsPlus() {
        final var dateRange1 = new DateRange(LocalDate.parse("2023-03-18"), LocalDate.parse("2023-03-18"));
        final var dateRange2 = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-18"));
        final var dateRange3 = new DateRange(LocalDate.parse("2023-03-18"), LocalDate.parse("2023-03-19"));

        assertThat(dateRange3.compareTo(dateRange1)).isPositive();
        assertThat(dateRange3.compareTo(dateRange2)).isPositive();
    }

    @Test
    void toString_sameDate_returnsOneDate() {
        final var today = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-17"));

        assertThat(today).asString().isEqualTo("[2023-03-17]");
    }

    @Test
    void toString_rangeOfDates_returnsOneRange() {
        final var today = new DateRange(LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-18"));

        assertThat(today).asString().isEqualTo("[2023-03-17,2023-03-18]");
    }

    @Test
    void compare_dateInRange_returnsZero() {
        final var dateRangeFeb2023 = new DateRange(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28"));

        assertThat(dateRangeFeb2023.compare(LocalDate.parse("2023-02-01"))).isZero();
        assertThat(dateRangeFeb2023.compare(LocalDate.parse("2023-02-15"))).isZero();
        assertThat(dateRangeFeb2023.compare(LocalDate.parse("2023-02-28"))).isZero();
    }

    @Test
    void compare_dateBeforeRange_returnsPositive() {
        final var dateRangeFeb2023 = new DateRange(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28"));

        assertThat(dateRangeFeb2023.compare(LocalDate.parse("2023-01-31"))).isPositive();
    }

    @Test
    void compare_dateAfterRange_returnsNegative() {
        final var dateRangeFeb2023 = new DateRange(LocalDate.parse("2023-02-01"), LocalDate.parse("2023-02-28"));

        assertThat(dateRangeFeb2023.compare(LocalDate.parse("2023-03-01"))).isNegative();
    }

    @Test
    void between_null_throwsNPE() {
        final var dateRange = DateRange.parse("2024-02");

        assertThatThrownBy(() -> dateRange.between(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("date");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            2024-01-31, false
            2024-02-01, true
            2024-02-29, true
            2024-03-01, false
            """)
    void between_variants_worksAsExpected(LocalDate test, boolean expectedBetween) {
        final var dateRange = DateRange.parse("2024-02");

        assertThat(dateRange.between(test)).isEqualTo(expectedBetween);
    }

    @Test
    void dates_sameDay_returnsSingleDate() {
        final var dateRange = new DateRange(LocalDate.parse("2023-03-18"), LocalDate.parse("2023-03-18"));

        final var dates = dateRange.dates().toList();

        assertThat(dates).hasSize(1)
                .contains(LocalDate.parse("2023-03-18"));
    }

    @Test
    void dates_rangeOf3Days_returnsAllThree() {
        final var dateRange = new DateRange(LocalDate.parse("2023-03-16"), LocalDate.parse("2023-03-18"));

        final var dates = dateRange.dates().toList();

        assertThat(dates).hasSize(3)
                .containsExactly(LocalDate.parse("2023-03-16"), LocalDate.parse("2023-03-17"), LocalDate.parse("2023-03-18"));
    }

    @Test
    void fromPreviousMonth_midMarch_returnsFebruary() {
        final var dateRange = DateRange.fromPreviousMonth(LocalDate.parse("2023-03-15"));

        assertThat(dateRange.from()).isEqualTo(LocalDate.parse("2023-02-01"));
        assertThat(dateRange.to()).isEqualTo(LocalDate.parse("2023-02-28"));
    }

    @Test
    void fromPreviousMonth_startJanuary_returnsDecPrevYear() {
        final var dateRange = DateRange.fromPreviousMonth(LocalDate.parse("2023-01-01"));

        assertThat(dateRange.from()).isEqualTo(LocalDate.parse("2022-12-01"));
        assertThat(dateRange.to()).isEqualTo(LocalDate.parse("2022-12-31"));
    }

    @Test
    void ofMonth_aDayInAMonth_returnsRangeForTheEntireMonth() {
        final var dateRange = DateRange.ofMonth(LocalDate.parse("2023-06-28"));

        assertThat(dateRange.from()).isEqualTo(LocalDate.parse("2023-06-01"));
        assertThat(dateRange.to()).isEqualTo(LocalDate.parse("2023-06-30"));
    }

    @Test
    void parse_entireMonth_returnsIt() {
        final var dateRange = DateRange.parse("2023-06");

        assertThat(dateRange.from()).isEqualTo(LocalDate.parse("2023-06-01"));
        assertThat(dateRange.to()).isEqualTo(LocalDate.parse("2023-06-30"));
    }

    @Test
    void parse_singleDate_returnsRangeOfOneDate() {
        final var dateRange = DateRange.parse("2023-06-01");

        assertThat(dateRange.from()).isEqualTo(LocalDate.parse("2023-06-01"));
        assertThat(dateRange.to()).isEqualTo(LocalDate.parse("2023-06-01"));
    }

    @Test
    void parse_ofRange_returnsRange() {
        final var dateRange = DateRange.parse("2023-06-01,2023-06-15");

        assertThat(dateRange.from()).isEqualTo(LocalDate.parse("2023-06-01"));
        assertThat(dateRange.to()).isEqualTo(LocalDate.parse("2023-06-15"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';',
            textBlock = """
            [2023-01-01,2023-02-28];2023-01-01;2023-02-28
            (2023-01-01,2023-02-28];2023-01-02;2023-02-28
            (2023-01-01,2023-02-28);2023-01-02;2023-02-27
            [2023-01-01,2023-02-28);2023-01-01;2023-02-27
            """)
    void parse_variants_returnsExpectedRange(String rangeAsString, LocalDate from, LocalDate to) {
        final var parsed = DateRange.parse(rangeAsString);

        final var expected = new DateRange(from, to);

        assertThat(parsed).isEqualTo(expected);
    }

    @Test
    void parse_null_throwsNPE() {
        assertThatThrownBy(() -> DateRange.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            crap
            """)
    void parse_unknownInput_throwsIllegalArgumentException(String badInput) {
        assertThatThrownBy(() -> DateRange.parse(badInput))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(badInput);
    }
}
