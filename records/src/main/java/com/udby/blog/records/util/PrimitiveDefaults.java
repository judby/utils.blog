package com.udby.blog.records.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrimitiveDefaults {
    public static Class<?> boxedType(Class<?> type) {
        if (type == boolean.class) {
            return Boolean.class;
        } else if (type == byte.class) {
            return Byte.class;
        } else if (type == char.class) {
            return Character.class;
        } else if (type == double.class) {
            return Double.class;
        } else if (type == float.class) {
            return Float.class;
        } else if (type == int.class) {
            return Integer.class;
        } else if (type == long.class) {
            return Long.class;
        } else if (type == short.class) {
            return Short.class;
        } else if (type == void.class) {
            return Void.class;
        }
        throw new IllegalArgumentException("type %s is not a known primitive: %s".formatted(type, type.isPrimitive()));
    }

    public static <T> T defaultValue(Class<T> type) {
        final Object value;
        if (type == byte.class) {
            value = (byte) 0;
        } else if (type == short.class) {
            value = (short) 0;
        } else if (type == char.class) {
            value = '\u0000';
        } else if (type == int.class) {
            value = 0;
        } else if (type == long.class) {
            value = 0L;
        } else if (type == float.class) {
            value = 0f;
        } else if (type == double.class) {
            value = 0d;
        } else if (type == boolean.class) {
            value = false;
        } else {
            value = null;
        }
        @SuppressWarnings("unchecked") final T returnValue = (T) value;
        return returnValue;
    }
}
