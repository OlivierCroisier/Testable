package net.mokatech.testable.sample.nested;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NestedTest {

    @Test
    void shouldGenerateDistinctInterfacesForNestedClassesWithTheSameSimpleName() {
        assertEquals(1, ((TestableLeft$Node) new Left.Node()).value$$());
        assertEquals("right", ((TestableRight$Node) new Right.Node()).value$$());
    }

}
