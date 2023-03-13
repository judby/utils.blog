package com.udby.blog.mutable.demo;

import org.junit.jupiter.api.Test;

class TopLevelDemoTest {
    @Test
    void demoIt() {
        TopLevel topLevel = new TopLevel("1");
        topLevel.children().add(new Child("2-1", topLevel, 2));
        topLevel.children().add(new Child("2-2", topLevel, 2));
        topLevel.ranges().add(new Range("4019200000000000", "4019209999999999"));
        final var child22 = topLevel.children().get(1);
        child22.children().add(new Child("3-1", child22, 3));
        child22.ranges().add(new Range("4119200000000000", "4119204999999999"));
        child22.ranges().add(new Range("4119205000000000", "4119209999999999"));
        final var child = child22.children().get(0);

        final var someForeignThing = new SomeForeignThing();
        someForeignThing.setValueForField(100200300);
        someForeignThing.setAnotherValue("ABC-123");

        if (someForeignThing != null) {
            topLevel.someField().setIfNull(someForeignThing.getValueForField());
            child.someField().setIfNull(someForeignThing.getAnotherValue());
            child.anotherField().setIfNull(someForeignThing.getCompletelyDifferentField());
        }

        System.out.println(topLevel);
    }
}