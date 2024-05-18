package com.udby.blog.records.demo.csv;

import com.udby.blog.records.util.PrimitiveDefaults;
import com.udby.blog.records.util.ReflectionUtils;

import java.io.BufferedReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.udby.blog.records.demo.csv.CsvHelper.EUROPEAN_DELIMITER;

public class CsvReader<T extends Record> implements Function<String, T> {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private final int columnCount;
    private final boolean[] columnIncluded;

    private final MethodHandle constructor;
    private final MethodHandle[] converters;
    private final Class<?>[] ctorTypes;
    private final char delimiterChar;
    private final String emptyLine;
    private final boolean europeanNumerics;
    private final boolean[] numericColumn;

    private CsvReader(String delimiter, boolean europeanNumerics, Class<T> recordType) {
        if (Objects.requireNonNull(delimiter, "delimiter").length() != 1) {
            throw new IllegalArgumentException("delimiter is one character, this is not: '%s'".formatted(delimiter));
        }
        this.delimiterChar = delimiter.charAt(0);
        this.europeanNumerics = europeanNumerics;

        final var recordComponents = recordType.getRecordComponents();

        this.emptyLine = delimiter.repeat(CsvHelper.columnCount(recordComponents) - 1);
        this.columnCount = CsvHelper.allColumnCount(recordComponents);
        // In the need of a mapToBoolean streaming operation
        this.columnIncluded = new boolean[columnCount];
        IntStream.range(0, columnCount).forEach(i ->
                columnIncluded[i] = recordComponents[i].getAnnotation(Column.class) == null || recordComponents[i].getAnnotation(Column.class).include());
        this.ctorTypes = Stream.of(recordComponents)
                .map(RecordComponent::getType)
                .toArray(Class[]::new);
        this.constructor = ReflectionUtils.invoke(() ->
                LOOKUP.findConstructor(recordType, MethodType.methodType(void.class, ctorTypes))
                        .asSpreader(Object[].class, columnCount));
        this.converters = Stream.of(recordComponents)
                .map(CsvReader::createConverter)
                .toArray(MethodHandle[]::new);
        // In the need of a mapToBoolean streaming operation
        this.numericColumn = new boolean[columnCount];
        IntStream.range(0, columnCount).forEach(i ->
        {
            final var type = ctorTypes[i];
            numericColumn[i] = Number.class.isAssignableFrom(type) ||
                    type.isPrimitive() && Number.class.isAssignableFrom(PrimitiveDefaults.boxedType(type));
        });
    }

    private static MethodHandle createConverter(RecordComponent recordComponent) {
        final var type = recordComponent.getType();
        return ReflectionUtils.invoke(() -> {
            if (type == String.class) {
                return LOOKUP.findVirtual(type, "toString", MethodType.methodType(String.class));
            }
            if (type == LocalDate.class) {
                return LOOKUP.findStatic(type, "parse", MethodType.methodType(LocalDate.class, CharSequence.class));
            }
            final Class<?> boxedType = type.isPrimitive() ? PrimitiveDefaults.boxedType(type) : type;
            if (type == Void.class) {
                return LOOKUP.findStatic(CsvReader.class, "failIfVoidHasValue", MethodType.methodType(Void.class, String.class));
            }
            try {
                // Go for "valueOf(String)" first
                return LOOKUP.findStatic(boxedType, "valueOf", MethodType.methodType(boxedType, String.class));
            } catch (NoSuchMethodException e) {
                // ..then try the ctor(String)
                return LOOKUP.findConstructor(boxedType, MethodType.methodType(void.class, String.class));
            }
        });
    }

    public CsvReader(Class<T> recordType) {
        this(recordType, EUROPEAN_DELIMITER);
    }

    public CsvReader(Class<T> recordType, String delimiter) {
        this(delimiter, EUROPEAN_DELIMITER.equals(delimiter), recordType);
    }

    @Override
    public T apply(String line) {
        if (Objects.requireNonNull(line, "line").equals(emptyLine)) {
            return null;
        }
        final var values = splitEscaped(line);
        final var ctorParams = new Object[columnCount];
        IntStream.range(0, columnCount)
                .forEach(i -> {
                    final var value = i < values.size() ? values.get(i) : null;
                    final var type = ctorTypes[i];
                    if (value == null || value.isEmpty() || !columnIncluded[i]) {
                        if (type.isPrimitive()) {
                            ctorParams[i] = PrimitiveDefaults.defaultValue(type);
                        } else {
                            ctorParams[i] = null;
                        }
                    } else {
                        if (numericColumn[i]) {
                            final var numericValue = europeanNumerics ? value.replace(',', '.') : value;
                            ctorParams[i] = convert(i, numericValue.strip());
                        } else {
                            ctorParams[i] = convert(i, value);
                        }
                    }
                });
        @SuppressWarnings("unchecked") final T t = (T) ReflectionUtils.invoke(() -> constructor.invoke(ctorParams));
        return t;
    }

    public Stream<T> process(Stream<String> lines) {
        return process(lines, false);
    }

    public <E extends Throwable> Stream<T> process(Stream<String> lines, boolean ignoreExceptions) throws E {
        try {
            return lines.map(line -> {
                try {
                    return apply(line);
                } catch (Exception e) {
                    if (ignoreExceptions) {
                        return null;
                    }
                    throw new InternalException(e);
                }
            });
        } catch (InternalException e) {
            @SuppressWarnings("unchecked") final var toThrow = (E) e.getCause();
            throw toThrow;
        }
    }

    public Stream<T> readFrom(BufferedReader reader) {
        return readFrom(reader, true, true);
    }

    public Stream<T> readFrom(BufferedReader reader, boolean skipHeader, boolean ignoreExceptions) {
        final var skippedHeader = new AtomicBoolean(!skipHeader);
        return process(reader.lines()
                        .filter(ignored -> skippedHeader.compareAndExchange(false, true)),
                ignoreExceptions);
    }

    private <E> E convert(int index, String value) {
        @SuppressWarnings("unchecked") final E e = (E) ReflectionUtils.invoke(() -> converters[index].invoke(value));
        return e;
    }

    List<String> splitEscaped(String line) {
        final var result = new ArrayList<String>(columnCount);
        final var sb = new StringBuilder();

        var inQuotes = false;
        var escaped = false;
        for (int i = 0; i < line.length(); i++) {
            final char ch = line.charAt(i);
            if (escaped) {
                sb.append(ch);
                escaped = false;
            } else if (inQuotes) {
                if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inQuotes = false;
                } else {
                    sb.append(ch);
                }
            } else {
                if (ch == '"') {
                    inQuotes = true;
                } else if (delimiterChar == ch) {
                    result.add(sb.toString());
                    // bail out quickly if we have what we need
                    if (result.size() == columnCount) {
                        return result;
                    }
                    sb.setLength(0);
                } else {
                    sb.append(ch);
                }
            }
        }

        result.add(sb.toString());

        return result;
    }

    static Void failIfVoidHasValue(String value) {
        if (value != null && !value.isBlank()) {
            throw new IllegalArgumentException("Attempt to convert '%s' to void".formatted(value));
        }
        return null;
    }

    static class InternalException extends RuntimeException {
        public InternalException(Throwable cause) {
            super(cause);
        }
    }
}
