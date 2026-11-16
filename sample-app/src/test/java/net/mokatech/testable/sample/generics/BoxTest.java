package net.mokatech.testable.sample.generics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxTest {

    @Test
    void shouldExposeGenericFields() {
        Box<String, Integer, Object> box = new Box<>("hello", List.of(1, 2), Map.of("key", List.of("v")), new Object[]{"a"}, new int[][]{{1}});

        @SuppressWarnings("unchecked")
        TestableBox<String, Integer, Object> testableBox = (TestableBox<String, Integer, Object>) box;
        String value = testableBox.value$$();
        List<? extends Integer> numbers = testableBox.numbers$$();
        Map<String, List<Object>> map = testableBox.map$$();

        assertEquals("hello", value);
        assertEquals(List.of(1, 2), numbers);
        assertEquals(Map.of("key", List.of("v")), map);
        assertArrayEquals(new Object[]{"a"}, testableBox.array$$());
        assertEquals(1, testableBox.grid$$()[0][0]);
    }

    @Test
    void shouldCallMethodsUsingTypeVariables() {
        Box<String, Integer, Object> box = new Box<>("b", List.of(), Map.of(), new Object[0], new int[0][]);
        @SuppressWarnings("unchecked")
        TestableBox<String, Integer, Object> testableBox = (TestableBox<String, Integer, Object>) box;
        assertTrue(testableBox.isGreaterThan$("a"));
    }

}
