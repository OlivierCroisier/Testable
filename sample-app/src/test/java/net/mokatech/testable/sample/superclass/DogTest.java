package net.mokatech.testable.sample.superclass;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DogTest {

    @Test
    void shouldAccessPrivateFieldViaAgent() {

        Dog dog =  new Dog("Woofy");

        TestableDog testableDog = (TestableDog) dog;
        assertEquals("Woofy", testableDog.name$$());
    }

}
