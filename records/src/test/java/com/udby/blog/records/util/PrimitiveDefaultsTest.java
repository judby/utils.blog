package com.udby.blog.records.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PrimitiveDefaultsTest {
    @Test
    void boxedType_notPrimitive_throwsIAE() {
        assertThatThrownBy(() -> PrimitiveDefaults.boxedType(String.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type class java.lang.String is not a known primitive: false");
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            boolean, Boolean, false
            byte, Byte, 0
            char, Character, '\u0000'
            double, Double, 0.0
            float, Float, 0.0
            int, Integer, 0
            long, Long, 0
            short, Short, 0
            """)
    void boxedType_variants_returnsBoxedType(String primitive, String expectedBoxedType, String expectedDefaultValue) throws Exception {
        final var boxedClass = Class.forName("java.lang.%s".formatted(expectedBoxedType));

        // We do this indirectly by looking up the corresponding factory method(of primitive) and check the parameter type...
        final var method = Stream.of(boxedClass.getDeclaredMethods())
                .filter(m -> "valueOf".equals(m.getName()))
                .filter(m -> m.getParameterTypes()[0] != String.class)
                .findAny()
                .orElseThrow();

        final Class<?> parameterType = method.getParameterTypes()[0];

        final Class<?> boxedType = PrimitiveDefaults.boxedType(parameterType);

        assertThat(boxedType).isEqualTo(boxedClass);
        assertThat(parameterType.isPrimitive()).isTrue();
        assertThat(parameterType.getName()).isEqualTo(primitive);

        // Let's just test the other way around too..
        final Object defaultValue = PrimitiveDefaults.defaultValue(parameterType);
        assertThat(defaultValue.toString()).isEqualTo(expectedDefaultValue);
    }

    @Test
    void boxedType_void_supportsVoidAsWell() throws Exception {
        final var boxedClass = Class.forName("java.lang.Void");

        assertThat(boxedClass).isNotNull();
    }
}
