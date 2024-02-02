package com.udby.blog.records.demo.csv;

import com.udby.blog.records.demo.csv.CsvWriter.Column;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvWriterTest {
    @Test
    void ctor_nullDelimiter_throwsNPE() {
        assertThatThrownBy(() -> new CsvWriter<>(TestRecord.class, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void ctor_delimiterNotOneChar_throwsIAE() {
        assertThatThrownBy(() -> new CsvWriter<>(TestRecord.class, "12"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delimiter is one character, this is not: '12'");
    }

    @Test
    void ctor_default_defaultsToEuro() {
        final var csvWriter = new CsvWriter<>(TestRecord.class);

        assertThat(csvWriter.header()).isEqualTo("COLUMN_INCLUDED;NAMED");
    }

    @Test
    void apply_null_returnsEmptyLine() {
        final var csvWriter = new CsvWriter<>(TestRecord.class);

        assertThat(csvWriter.apply(null)).isEqualTo(";");
    }

    @Test
    void apply_multipleTypes_returnsCorrectlyMappedValuesEuropeanNumerics() {
        final var csvWriter = new CsvWriter<>(TestWithTypes.class);

        final var applied = csvWriter.apply(new TestWithTypes(
                1,
                2L,
                0.1d,
                new BigDecimal("4.21"),
                new BigInteger("12345678901234567890123456789"),
                null,
                true,
                TestEnum.VALUE_A,
                "Some string to \"escape\"",
                LocalDate.parse("2024-01-01")));

        assertThat(applied).isEqualTo("1;2;0,1;4,21;12345678901234567890123456789;;TRUE;VALUE_A;\"Some string to \\escape\\\";2024-01-01");
    }

    @Test
    void apply_multipleTypes_returnsCorrectlyMappedValuesStandardNumerics() {
        final var csvWriter = new CsvWriter<>(TestWithTypes.class, ",");

        final var applied = csvWriter.apply(new TestWithTypes(
                -1,
                -2L,
                -0.1d,
                new BigDecimal("-4.21"),
                new BigInteger("-12345678901234567890123456789"),
                null,
                false,
                TestEnum.VALUE_B,
                "Some string to \"escape\"",
                LocalDate.parse("2024-02-02")));

        assertThat(applied).isEqualTo("-1,-2,-0.1,-4.21,-12345678901234567890123456789,,FALSE,VALUE_B,\"Some string to \\escape\\\",2024-02-02");
    }

    @Test
    void writeTo_throwingIOException_throwsUncheckedIOException() {
        final var writer = new BufferedWriter(new StringWriter()){
            @Override
            public void write(String str) throws IOException {
                throw new IOException("failed");
            }
        };

        final var csvWriter = new CsvWriter<>(TestRecord.class);

        assertThatThrownBy(() -> csvWriter.writeTo(writer, Stream.empty()))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("failed");
    }

    @Test
    void writeTo_empty_writesHeaderOnly() throws IOException {
        final var output = new StringWriter();
        try (final var writer = new BufferedWriter(output)) {
            final var csvWriter = new CsvWriter<>(TestRecord.class);

            csvWriter.writeTo(writer, Stream.empty());
        }

        assertThat(output.toString().lines()).containsExactly("COLUMN_INCLUDED;NAMED");
    }

    @Test
    void writeTo_noHeader_writesContentOnly() throws IOException {
        final var output = new StringWriter();
        try (final var writer = new BufferedWriter(output)) {
            final var csvWriter = new CsvWriter<>(TestRecord.class);

            csvWriter.writeTo(writer, Stream.of(new TestRecord("Some value", "excluded", "named")), false);
        }

        assertThat(output.toString().lines()).containsExactly("\"Some value\";\"named\"");
    }

    record TestRecord(
            String columnIncluded,
            @Column(include = false) String columnNotIncluded,
            @Column("NAMED") String namedColumn) {
    }

    record TestWithTypes(
            int intValue,
            long longValue,
            double doubleValue,
            BigDecimal bigDecimalValue,
            BigInteger bigIntegerValue,
            Void alwaysNull,
            boolean booleanValue,
            TestEnum enumValue,
            String stringValue,
            LocalDate dateValue) {
    }

    enum TestEnum {
        VALUE_A,
        VALUE_B
    }
}
