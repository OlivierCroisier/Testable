package net.mokatech.testable.sample;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTest {

    @Test
    void shouldAccessPrivateFieldViaAgent() {

        // Test Lombok (and other annotation processors) still works
        Address address = new Address();
        address.setStreet("Some Street");

        // Test the "untestable" object
        User user = new User("James Bond", "password007");

        // Non testable !
        // assertEquals("James Bond", user.secretName);
        // assertEquals("James Bond", user.getSecretName());

        //region (...) Testable
        if (user instanceof TestableUser) {
            TestableUser testableUser = (TestableUser) user;
            assertEquals("James Bond", testableUser.secretName$());
            assertEquals("password007", testableUser.password$());
        } else {
            System.out.println("User is not an instance of TestableUser ! The agent may not be attached.");
        }
        //endregion
    }

}