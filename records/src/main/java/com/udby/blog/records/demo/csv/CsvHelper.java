package com.udby.blog.records.demo.csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.RecordComponent;
import java.util.stream.Stream;

public class CsvHelper {
    public static final String EUROPEAN_DELIMITER = ";";

    static Stream<RecordComponent> columnsFor(RecordComponent[] recordComponents) {
        return Stream.of(recordComponents)
                .filter(component -> component.getAnnotation(Column.class) == null || component.getAnnotation(Column.class).include());
    }

    static int columnCount(RecordComponent[] recordComponents) {
        return (int) columnsFor(recordComponents).count();
    }

    static int allColumnCount(RecordComponent[] recordComponents) {
        return (int) Stream.of(recordComponents).count();
    }

    static void handleIOException(OperationThrowingIOException operation) {
        try {
            operation.invoke();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    interface OperationThrowingIOException {
        void invoke() throws IOException;
    }
}
