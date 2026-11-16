package net.mokatech.testable.sample.methods;

import net.mokatech.testable.api.Testable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

/** Non-static methods of a testable class are exposed whatever their visibility, unless their signature cannot be expressed in the interface. */
@Testable
public class Calculator {

    private int count;
    private int total;
    int lastValue;

    int doubled(int value) {
        return 2 * value;
    }

    public int negated(int value) {
        return -value;
    }

    private int add(int a, int b) {
        return a + b;
    }

    // Overloads
    private String format(int value) {
        return "int:" + value;
    }

    private String format(String value) {
        return "string:" + value;
    }

    // Generic method: its type parameter is redeclared on the interface method
    private <R extends Comparable<R>> R max(List<R> values) {
        return Collections.max(values);
    }

    private int sum(int... values) {
        return IntStream.of(values).sum();
    }

    private int parse(String text) throws IOException {
        if (text.isBlank()) {
            throw new IOException("blank");
        }
        return Integer.parseInt(text);
    }

    // Inaccessible checked exception: declared as its closest accessible superclass (Exception)
    private void fail() throws SecretException {
        throw new SecretException();
    }

    private void accumulate(int value) {
        count++;
        total += value;
        lastValue = value;
    }

    // Inaccessible parameter type: ignored
    private String describe(Secret secret) {
        return "secret";
    }

    // Same name as the field: no clash, the field is exposed as count$$() and the method as count$()
    private int count() {
        return count;
    }

    // Static: ignored
    private static int twice(int value) {
        return 2 * value;
    }

    private static class Secret {
    }

    private static class SecretException extends Exception {
    }
}
