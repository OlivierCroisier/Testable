package net.mokatech.testable.sample.superclass1;

import net.mokatech.testable.sample.TestableUser;
import net.mokatech.testable.sample.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DogTest {

    @Test
    void shouldAccessPrivateFieldViaAgent() {

        Dog dog =  new Dog("Woofy");

        TestableDog testableDog = (TestableDog) dog;
        //assertEquals("Dog", testableDog.type$());
        assertEquals("Woofy", testableDog.name$());
    }

}