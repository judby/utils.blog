package com.udby.blog.mutable.demo;

import com.udby.blog.mutable.simple.Mutable;

import java.util.Objects;

public record RecordOverridingEqualsHashCode(String s, Mutable<String> mutable) {
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof RecordOverridingEqualsHashCode other
                && Objects.equals(s, other.s);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(s);
    }
}
