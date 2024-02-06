package com.udby.blog.mutable.demo;

import java.util.List;
import java.util.Set;

sealed interface HierarchyElement permits Child, TopLevel {
    String id();

    HierarchyElement parent();

    int level();

    <C extends HierarchyElement> List<C> children();

    Set<Range> ranges();
}
