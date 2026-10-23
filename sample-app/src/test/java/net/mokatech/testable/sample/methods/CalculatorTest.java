package net.mokatech.testable.sample.methods;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculatorTest {

    private final TestableCalculator calculator = (TestableCalculator) new Calculator();

    @Test
    void shouldCallPrivateMethods() {
        assertEquals(5, calculator.add$(2, 3));
        assertEquals("int:1", calculator.format$(1));
        assertEquals("string:a", calculator.format$("a"));
        assertEquals(7, calculator.max$(List.of(3, 7, 5)));
        assertEquals(6, calculator.sum$(1, 2, 3));
        assertEquals(0, calculator.sum$());
    }

    @Test
    void shouldCallPrivateMethodsChangingState() {
        calculator.accumulate$(5);
        calculator.accumulate$(7);

        assertEquals(2, calculator.count$$());
        assertEquals(12, calculator.total$$());
    }

    @Test
    void shouldExposeFieldAndMethodSharingAName() {
        calculator.accumulate$(1);

        assertEquals(1, calculator.count$$());
        assertEquals(1, calculator.count$());
    }

    @Test
    void shouldPropagateExceptions() throws IOException {
        assertEquals(42, calculator.parse$("42"));
        assertThrows(IOException.class, () -> calculator.parse$(" "));

        Exception exception = assertThrows(Exception.class, calculator::fail$);
        assertEquals("SecretException", exception.getClass().getSimpleName());
    }

    @Test
    void shouldIgnoreInexpressibleAmbiguousAndStaticMethods() {
        Set<String> methods = Arrays.stream(TestableCalculator.class.getDeclaredMethods())
                                    .map(method -> method.getName() + "/" + method.getParameterCount())
                                    .collect(Collectors.toSet());

        assertFalse(methods.contains("describe$/1"));
        assertFalse(methods.contains("twice$/1"));
        assertFalse(methods.contains("legacy$$/0"));
    }

    @Test
    void shouldIgnoreNonPrivateMembers() {
        Set<String> methods = Arrays.stream(TestableCalculator.class.getDeclaredMethods())
                                    .map(method -> method.getName())
                                    .collect(Collectors.toSet());

        assertFalse(methods.contains("lastValue$$"));
        assertFalse(methods.contains("doubled$"));
    }

}
