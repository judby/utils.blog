package com.udby.blog.mutable.demo;

import com.udby.blog.mutable.simple.Mutable;
import com.udby.blog.mutable.simple.MutableOverridingEqualsHashCode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class SurprisesTest {
    @Test
    void recordWithMutableInSetSurprise() {
        // So how does a RecordWithMutable behave in a Set...
        final var set = new HashSet<RecordWithMutableA>();
        // Let's add 2 seemingly identical records to the set...
        set.add(new RecordWithMutableA("A", new Mutable<>("A")));
        set.add(new RecordWithMutableA("A", new Mutable<>("A")));
        // We would think there is only one item in the set, but from an identity perspective they are different
        // since they refer to two different instances of Mutable:
        assertThat(set).hasSize(2);
        //... so if they reference the same Mutable, we're good?
        set.clear();
        final var mutableA = new Mutable<>("A");
        set.add(new RecordWithMutableA("A", mutableA));
        set.add(new RecordWithMutableA("A", mutableA));
        // Right:
        assertThat(set).hasSize(1);
    }

    @Test
    void recordOverridesEqualsAndHashCodeLessSurprising() {
        // So if we have a record with a mutable field implementing equals and hashCode
        // to exclude the mutable field, we have less surprises...
        final var set = new HashSet<RecordOverridingEqualsHashCode>();
        // Let's add 2 seemingly identical records to the set...
        set.add(new RecordOverridingEqualsHashCode("A", new Mutable<>("A")));
        set.add(new RecordOverridingEqualsHashCode("A", new Mutable<>("A")));
        // Then they are treated equal, so we have:
        assertThat(set).hasSize(1);
    }

    @Test
    void mutableOverridingEqualsAndHashCodeSurprises() {
        // Now what if our Mutable has "sane" implementations of equals and hashCode?...
        // Let's add 2 seemingly identical record to the set...
        final var set = new HashSet<RecordWithMutableB>();
        set.add(new RecordWithMutableB("A", new MutableOverridingEqualsHashCode<>("A")));
        set.add(new RecordWithMutableB("A", new MutableOverridingEqualsHashCode<>("A")));
        // Lo and behold - IT WORKS!!
        assertThat(set).hasSize(1);
        // So why are we discussing surprises?...
        // Well, the mutable is... mutable so lets mutate it :-D
        set.iterator().next().mutable().set("B");
        set.add(new RecordWithMutableB("A", new MutableOverridingEqualsHashCode<>("A")));
        // Is this surprising?...
        assertThat(set).hasSize(2);
        // Now what...
        set.iterator().next().mutable().set("A");
        // What about this?
        assertThat(set).hasSize(2);
        // Let's get them as a list, they cannot be identical since Set would never allow that?
        final var list = set.stream().toList();
        final var item1 = list.get(0);
        final var item2 = list.get(1);
        // Are they different?
        assertThat(item1).isNotSameAs(item2);
        // BUT THEY ARE EQUAL!?
        assertThat(item1).isEqualTo(item2);
    }
}
