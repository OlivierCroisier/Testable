package net.mokatech.testable.sample.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ColorTest {

    // Enums are final (or sealed): javac rejects a direct cast to an interface they do not implement at compile time, hence the cast through Object
    @Test
    void shouldExposeEnumFields() {
        assertEquals("#FF0000", ((TestableColor) (Object) Color.RED).hex$$());
    }

    @Test
    void shouldExposeEnumFieldsOfConstantsWithABody() {
        assertEquals("#0000FF", ((TestableColor) (Object) Color.BLUE).hex$$());
    }

}
