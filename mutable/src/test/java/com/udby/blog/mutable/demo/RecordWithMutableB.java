package com.udby.blog.mutable.demo;

import com.udby.blog.mutable.simple.MutableOverridingEqualsHashCode;

public record RecordWithMutableB(String s, MutableOverridingEqualsHashCode<String> mutable) {
}
