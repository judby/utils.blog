package com.udby.blog.records.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.UndeclaredThrowableException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReflectionUtils {
    public static <T> T invoke(ReflectiveOperation<T> operation) {
        try {
            return operation.invoke();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        } catch (Error | RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @FunctionalInterface
    interface ReflectiveOperation<T> {
        T invoke() throws Throwable;
    }
}
