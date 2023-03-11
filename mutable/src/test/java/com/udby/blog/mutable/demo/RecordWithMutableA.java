package com.udby.blog.mutable.demo;

import com.udby.blog.mutable.simple.Mutable;

public record RecordWithMutableA(String s, Mutable<String> mutable) {
}
