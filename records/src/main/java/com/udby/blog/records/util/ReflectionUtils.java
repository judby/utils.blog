package com.udby.blog.records.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectionUtils {
    public static <T, E extends Throwable> T invoke(ReflectiveOperation<T> operation) throws E {
        try {
            return operation.invoke();
        } catch (Throwable e) {
            @SuppressWarnings("unchecked") final E toThrow = (E) e;
            throw toThrow;
        }
    }

    @FunctionalInterface
    public interface ReflectiveOperation<T> {
        T invoke() throws Throwable;
    }
}
