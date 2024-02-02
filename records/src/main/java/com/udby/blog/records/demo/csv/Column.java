package com.udby.blog.records.demo.csv;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column {
    boolean include() default true;

    String value() default DEFAULT_NAME;

    String DEFAULT_NAME = ":.the-default-column-name;,";
}
