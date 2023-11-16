package com.udby.blog.records.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.RecordComponent;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecordMapper {
    public static <R extends Record> Map<String, Object> map(R record) {
        if (record == null) {
            return Map.of();
        }
        final var mapped = new LinkedHashMap<String, Object>();
        for (final var recordComponent : record.getClass().getRecordComponents()) {
            final var name = recordComponent.getName();
            final var value = ReflectionUtils.invoke(() -> recordComponent.getAccessor().invoke(record));
            if (value instanceof Record recordValue) {
                mapped.put(name, map(recordValue));
            } else {
                mapped.put(name, value);
            }
        }
        return mapped;
    }

    public static <R extends Record> R map(Class<R> recordType, Map<String, Object> fields) {
        final var recordComponents = recordType.getRecordComponents();
        final var ctorParams = new Object[recordComponents.length];
        final var ctorTypes = new Class<?>[recordComponents.length];
        for (var i = 0; i < recordComponents.length; i++) {
            final var value = fields.get(recordComponents[i].getName());
            final var type = ctorTypes[i] = recordComponents[i].getType();
            if (type.isRecord() && value instanceof Map map) {
                ctorParams[i] = map((Class<? extends Record>) type, (Map<String, Object>) map);
            } else if (type.isPrimitive() && value == null) {
                ctorParams[i] = defaultPrimitiveValue(type);
            } else {
                ctorParams[i] = value;
            }
        }
        return ReflectionUtils.invoke(
                () -> recordType.getDeclaredConstructor(ctorTypes)
                        .newInstance(ctorParams));
    }

    private static Object defaultPrimitiveValue(Class<?> type) {
        if (type == byte.class) {
            return (byte)0;
        } else if (type == short.class) {
            return (short)0;
        } else if (type == char.class) {
            return '\u0000';
        } else if (type == int.class) {
            return 0;
        } else if (type == long.class) {
            return 0L;
        } else if (type == float.class) {
            return 0f;
        } else if (type == double.class) {
            return 0d;
        } else if (type == boolean.class) {
            return false;
        } else {
            return null;
        }
    }
}
