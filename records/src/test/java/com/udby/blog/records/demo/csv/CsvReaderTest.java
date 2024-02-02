package com.udby.blog.records.demo.csv;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static com.udby.blog.records.demo.csv.TestEnum.VALUE_A;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LOCAL_DATE;
import static org.junit.jupiter.api.Assertions.*;

class CsvReaderTest {
    @Test
    void apply_strings_works() {
        final var csvReader = new CsvReader<>(TestRecord.class);

        final var record = csvReader.apply("a;b;c;d");

        assertThat(record).isEqualTo(new TestRecord("a", null, "c"));
    }

    @Test
    void apply_allBlanks_works() {
        final var csvReader = new CsvReader<>(TestWithTypes.class);

        final var record = csvReader.apply(";;;;;;;;\"\";");

        System.out.println(record);
    }

    @Test
    void apply_complex_works() {
        final var csvReader = new CsvReader<>(TestWithTypes.class);

        final var record = csvReader.apply("1;2;3,1;4.01;1234 ;;TRUE;VALUE_A;some-string ;2024-02-02");

        assertThat(record).isEqualTo(new TestWithTypes(
                1,
                2,
                3.1,
                new BigDecimal("4.01"),
                new BigInteger("1234"),
                null,
                true,
                VALUE_A,
                "some-string ",
                LocalDate.parse("2024-02-02")
        ));
    }

    @Test
    void apply_tooFewColumns_works() {
        final var csvReader = new CsvReader<>(TestWithTypes.class);

        final var record = csvReader.apply("1;2;3,1;4.01;1234;;TRUE;VALUE_A;some-string");

        assertThat(record).isEqualTo(new TestWithTypes(
                1,
                2,
                3.1,
                new BigDecimal("4.01"),
                new BigInteger("1234"),
                null,
                true,
                VALUE_A,
                "some-string",
                null
        ));
    }

    @Test
    void apply_valueForVoid_throwsException() {
        final var csvReader = new CsvReader<>(TestWithTypes.class);

        assertThatThrownBy(() -> csvReader.apply(";;;;;this is void;;"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Attempt to convert 'this is void' to void");
    }

    @Test
    void splitEscaped_tooLong_returnsNecessaryColumnsOnly() {
        final var csvReader = new CsvReader<>(TestRecord.class);

        final var strings = csvReader.splitEscaped("a;b;c;;e;f;g;h");

        assertThat(strings).containsExactly("a", "b", "c", "");
    }

    @Test
    void splitEscaped_escapedString_returnsUnescapedColumns() {
        final var csvReader = new CsvReader<>(TestRecord.class);

        final var strings = csvReader.splitEscaped("\";\\\",\";b,c;\"d;e\"");

        assertThat(strings).containsExactly(";\",", "b,c", "d;e");
    }

    @Test
    void readFrom_ignoreException_ignored() throws IOException {
        final var csvReader = new CsvReader<>(TestWithTypes.class);

        final TestWithTypes result;
        try (final var reader = new BufferedReader(new StringReader(";;;;;this is void;;"))) {
            result = csvReader.readFrom(reader, false, true)
                    .toList()
                    .get(0);
        }
        assertThat(result).isNull();
    }

    @Test
    void readFrom_headerIgnored_parsed() throws IOException {
        final var csvReader = new CsvReader<>(TestWithTypes.class, ",");

        final var stringInput = new StringReader("""
                this-is-a-header-and-is.ignored
                -1,-2,-3,-4.01,123456789012345678901234567890123456789012345678901234567890,,,,asdf
                """);
        final List<TestWithTypes> results;
        try (final var reader = new BufferedReader(stringInput)) {
            results = csvReader.readFrom(reader, true, false)
                    .toList();
        }

        assertThat(results).hasSize(1);
        assertThat(results.get(0)).isEqualTo(new TestWithTypes(
                -1,
                -2,
                -3,
                new BigDecimal("-4.01"),
                new BigInteger("123456789012345678901234567890123456789012345678901234567890"),
                null,
                false,
                null,
                "asdf",
                null));
    }
}
