package com.udby.blog.records.demo.csv;

record TestRecord(
        String columnIncluded,
        @Column(include = false) String columnNotIncluded,
        @Column("NAMED") String namedColumn,
        @Column(include = false) Void thisIsVoid) {
    public TestRecord(String columnIncluded, String columnNotIncluded, String namedColumn) {
        this(columnIncluded, columnNotIncluded, namedColumn, null);
    }
}
