package com.udby.blog.records.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PrimitiveDefaults {
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
