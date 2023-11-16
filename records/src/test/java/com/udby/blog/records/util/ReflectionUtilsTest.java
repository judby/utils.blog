package com.udby.blog.records.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.UndeclaredThrowableException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReflectionUtilsTest {

    @Test
    void invoke_reflectiveOperationException_throwsIllegalStateException() {
        final var reflectiveOperationException = new ReflectiveOperationException();

        assertThatThrownBy(() ->
                        ReflectionUtils.invoke(() -> {
                            throw reflectiveOperationException;
                        })
                )
                .isInstanceOf(IllegalStateException.class)
                .cause()
                .isSameAs(reflectiveOperationException);
    }

    @Test
    void invoke_error_throwsSame() {
        final var error = new Error();

        assertThatThrownBy(() ->
                        ReflectionUtils.invoke(() -> {
                            throw error;
                        })
                )
                .isSameAs(error);
    }

    @Test
    void invoke_runtimeException_throwsSame() {
        final var runtimeException = new RuntimeException();

        assertThatThrownBy(() ->
                        ReflectionUtils.invoke(() -> {
                            throw runtimeException;
                        })
                )
                .isSameAs(runtimeException);
    }

    @Test
    void invoke_throwable_throwsUndeclaredThrowableException() {
        final var throwable = new Throwable();

        assertThatThrownBy(() ->
                        ReflectionUtils.invoke(() -> {
                            throw throwable;
                        })
                )
                .isInstanceOf(UndeclaredThrowableException.class)
                .cause()
                .isSameAs(throwable);
    }
}
