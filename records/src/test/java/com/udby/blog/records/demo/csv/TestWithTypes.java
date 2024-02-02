package com.udby.blog.records.demo.csv;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

record TestWithTypes(
        int intValue,
        long longValue,
        double doubleValue,
        BigDecimal bigDecimalValue,
        BigInteger bigIntegerValue,
        Void alwaysNull,
        boolean booleanValue,
        TestEnum enumValue,
        String stringValue,
        LocalDate dateValue) {
}
