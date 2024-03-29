package com.udby.blog.records.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
            if (type.isRecord() && value instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked") final Object v = map((Class<? extends Record>) type, (Map<String, Object>) map);
                ctorParams[i] = v;
            } else if (type.isPrimitive() && value == null) {
                ctorParams[i] = PrimitiveDefaults.defaultValue(type);
            } else {
                ctorParams[i] = value;
            }
        }
        return ReflectionUtils.invoke(
                () -> recordType.getDeclaredConstructor(ctorTypes)
                        .newInstance(ctorParams));
    }
}
