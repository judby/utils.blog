package com.udby.blog.records.demo.csv;

import com.udby.blog.records.util.ReflectionUtils;

import java.io.BufferedWriter;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvWriter<T extends Record> implements Function<T, String> {
    private final String delimiter;
    private final String emptyLine;
    private final boolean europeanNumerics;
    private final String header;
    private final RecordComponent[] recordComponents;

    private static final Pattern ESCAPE_STRING = Pattern.compile("\"");
    private static final Pattern UPPER_CASE_UNDERSCORE = Pattern.compile("([a-z0-9])([A-Z]+)");

    private CsvWriter(String delimiter, boolean europeanNumerics, String header, Class<T> recordType) {
        if (Objects.requireNonNull(delimiter, "delimiter").length() != 1) {
            throw new IllegalArgumentException("delimiter is one character, this is not: '%s'".formatted(delimiter));
        }
        this.delimiter = delimiter;
        this.europeanNumerics = europeanNumerics;
        this.header = header;

        this.recordComponents = recordType.getRecordComponents();
        this.emptyLine = delimiter.repeat(CsvHelper.columnCount(recordComponents) - 1);
    }

    public CsvWriter(Class<T> recordType) {
        this(recordType, CsvHelper.EUROPEAN_DELIMITER);
    }

    public CsvWriter(Class<T> recordType, String delimiter) {
        this(delimiter, CsvHelper.EUROPEAN_DELIMITER.equals(delimiter), headerFromRecord(recordType, delimiter), recordType);
    }

    private static <T extends Record> String headerFromRecord(Class<T> recordType, String delimiter) {
        return CsvHelper.columnsFor(recordType.getRecordComponents())
                .map(component -> {
                    final var column = component.getAnnotation(Column.class);
                    if (column != null && !Column.DEFAULT_NAME.equals(column.value())) {
                        return column.value();
                    }
                    return UPPER_CASE_UNDERSCORE.matcher(component.getName())
                            .replaceAll("$1_$2")
                            .toUpperCase(Locale.ROOT);
                })
                .collect(Collectors.joining(delimiter));
    }

    public String header() {
        return header;
    }

    @Override
    public String apply(T record) {
        if (record == null) {
            return emptyLine;
        } else {
            return CsvHelper.columnsFor(recordComponents)
                    .map(recordComponent -> valueOfField(recordComponent, record))
                    .map(this::map)
                    .collect(Collectors.joining(delimiter));
        }
    }

    public void writeTo(BufferedWriter writer, Stream<T> recordStream) {
        writeTo(writer, recordStream, true);
    }

    public void writeTo(BufferedWriter writer, Stream<T> recordStream, boolean applyHeader) {
        if (applyHeader) {
            writeLine(writer, header());
        }
        recordStream.map(this)
                .forEach(line -> writeLine(writer, line));
    }

    private void writeLine(BufferedWriter writer, String line) {
        CsvHelper.handleIOException(() -> {
            writer.write(line);
            writer.newLine();
        });
    }

    private <E> E valueOfField(RecordComponent recordComponent, T record) {
        @SuppressWarnings("unchecked") final var value = (E) ReflectionUtils.invoke(() -> recordComponent.getAccessor().invoke(record));
        return value;
    }

    private String map(Object value) {
        return switch (value) {
            case Boolean bool -> bool ? "TRUE" : "FALSE";
            case Enum<?> enm -> enm.name();
            case LocalDate localDate -> localDate.toString();
            case null -> "";
            case Number number -> europeanNumerics ? number.toString().replace('.', ',') : number.toString();
            default -> escaped(value.toString());
        };
    }

    private String escaped(String input) {
        return "\"%s\"".formatted(ESCAPE_STRING.matcher(input).replaceAll("\\\\"));
    }
}
