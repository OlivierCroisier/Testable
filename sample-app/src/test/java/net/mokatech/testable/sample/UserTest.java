package net.mokatech.testable.sample;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UserTest {

    @Test
    void shouldAccessPrivateFieldsViaAgent() {
        User user = new User("James Bond", "password007", new Address());

        // Non testable !
        // assertEquals("James Bond", user.secretName);
        // assertEquals("James Bond", user.password);

        // region (...) Testable
        TestableUser testableUser = (TestableUser) user;
        assertEquals("James Bond", testableUser.secretName$$());
        assertEquals("password007", testableUser.password$$());
        // endregion
    }

    @Test
    void shouldReturnTheTestableInterfaceOfAnnotatedFields() {
        // Lombok (and other annotation processors) still work
        Address address = new Address();
        address.setStreet("Some Street");
        address.setCity("Some City");
        User user = new User("James Bond", "password007", address);

        TestableAddress testableAddress = ((TestableUser) user).address$$();
        assertSame(address, testableAddress);
        assertEquals("Some Street", testableAddress.street$$());
        assertEquals("Some City", testableAddress.city$$());
    }

}
