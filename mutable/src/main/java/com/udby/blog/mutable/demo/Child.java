package com.udby.blog.mutable.demo;

import com.udby.blog.mutable.simple.Mutable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record Child(String id, HierarchyElement parent, int level, List<Child> children, Set<Range> ranges, Mutable<String> someField, Mutable<String> anotherField) implements HierarchyElement {
    public Child(String id, HierarchyElement parent, int level) {
        this(id, parent, level, new ArrayList<>(), new LinkedHashSet<>(), new Mutable<>(), new Mutable<>());
    }

    /*
     * equals and hashCode excluding the mutable elements
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof Child other
                && Objects.equals(id, other.id)
                && level == other.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, level);
    }

    /*
     * toString excluding parent (including only the id) or we have an infinite loop
     */
    @Override
    public String toString() {
        return "Child[id=%s,parent.id=%s,level=%d,children=%s,ranges=%s,someField=%s,anotherField=%s]".formatted(id, parent.id(), level, children, ranges, someField, anotherField);
    }
}
