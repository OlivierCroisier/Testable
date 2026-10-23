package net.mokatech.testable.sample.methods;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartialTest {

    @Test
    void shouldCallAnnotatedPrivateMethod() {
        assertEquals("Hello Bob from partial", ((TestablePartial) new Partial()).greet$("Bob"));
    }

    @Test
    void shouldOnlyExposeAnnotatedPrivateMethods() {
        Set<String> methods = Arrays.stream(TestablePartial.class.getDeclaredMethods())
                                    .map(java.lang.reflect.Method::getName)
                                    .collect(Collectors.toSet());

        assertEquals(Set.of("greet$"), methods);
    }

}
