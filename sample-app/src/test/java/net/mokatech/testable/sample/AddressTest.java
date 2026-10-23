package net.mokatech.testable.sample;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AddressTest {

    @Test
    void shouldExposeAnnotatedField() {
        Address address = new Address();
        address.setStreet("Some Street");

        assertEquals("Some Street", ((TestableAddress) address).street$$());
    }

    @Test
    void shouldNotExposeOtherFields() {
        assertThrows(NoSuchMethodException.class, () -> TestableAddress.class.getMethod("city$$"));
    }

}
