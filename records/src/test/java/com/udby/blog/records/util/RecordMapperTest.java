package com.udby.blog.records.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

class RecordMapperTest {

    @Test
    void map_null_returnsEmptyMap() {
        final Map<String, ?> map = RecordMapper.map(null);

        assertThat(map).isNotNull().isEmpty();
    }

    @Test
    void map_sunshine_succeeds() {
        final Map<String, Object> map = RecordMapper.map(new TestRecord(42, "666"));

        assertThat(map).isNotNull()
                .hasSize(2)
                .contains(entry("anInt", 42), entry("aString", "666"));
    }

    @Test
    void map_withNullValue_succeeds() {
        final Map<String, Object> map = RecordMapper.map(new TestRecord(42, null));

        assertThat(map).isNotNull().hasSize(2);
        assertThat(map.get("aString")).isNull();
    }

    @Test
    void map_deep_succeeds() {
        final var recordWithRecord = new RecordWithRecord("B", new TestRecord(42, "zz"));

        final var map = RecordMapper.map(recordWithRecord);

        assertThat(map).isNotNull().hasSize(2);
        assertThat(map.get("bString")).isEqualTo("B");
        final var testRecordMap = map.get("testRecord");
        assertThat(testRecordMap).isNotNull().isInstanceOf(Map.class);
    }

    @Test
    void map_testRecordPrimitiveDefaults_succeeds() {
        final var testRecord = RecordMapper.map(TestRecord.class, Map.of());

        assertThat(testRecord.anInt).isZero();
        assertThat(testRecord.aString).isNull();
    }

    @Test
    void map_testRecordSimple_succeeds() {
        final var testRecord = RecordMapper.map(TestRecord.class, Map.of("anInt", 42, "aString", "hurra"));

        assertThat(testRecord.anInt).isEqualTo(42);
        assertThat(testRecord.aString).isEqualTo("hurra");
    }

    @Test
    void map_testRecodrWithRecord_succeeds() {
        final var map = Map.of("testRecord", Map.of("anInt", 42, "aString", "hurra"), "bString", "bravo");

        final var recordWithRecord = RecordMapper.map(RecordWithRecord.class, map);
        assertThat(recordWithRecord.testRecord.anInt).isEqualTo(42);
        assertThat(recordWithRecord.testRecord.aString).isEqualTo("hurra");
        assertThat(recordWithRecord.bString).isEqualTo("bravo");
    }

    record TestRecord(int anInt, String aString) {
    }

    record RecordWithRecord(String bString, TestRecord testRecord) {
    }
}
