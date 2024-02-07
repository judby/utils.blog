package com.udby.blog.mutable.demo;

import com.udby.blog.mutable.simple.Mutable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record TopLevel(String id, List<Child> children, Set<Range> ranges, Mutable<Integer> someField) implements HierarchyElement {
    public TopLevel(String id) {
        this(id, new ArrayList<>(), new LinkedHashSet<>(), new Mutable<>());
    }

    @Override
    public TopLevel parent() {
        return this;
    }

    @Override
    public int level() {
        return 1;
    }

    /*
     * equals and hashCode excluding the mutable elements
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof TopLevel other
                && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /*
     * toString excluding parent or we have an infinite loop
     */
    @Override
    public String toString() {
        return "TopLevel[id=%s,children=%s,ranges=%s,someField=%s]".formatted(id, children, ranges, someField);
    }
}
