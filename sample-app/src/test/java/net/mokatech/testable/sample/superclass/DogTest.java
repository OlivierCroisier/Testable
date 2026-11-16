package net.mokatech.testable.sample.superclass;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DogTest {

    @Test
    void shouldAccessPrivateFieldViaAgent() {
        TestableDog testableDog = (TestableDog) new Dog("Woofy");
        assertEquals("Woofy", testableDog.name$$());
    }

    // TestableDog extends the interface of its testable superclass
    @Test
    void shouldReadInheritedFieldsThroughTheSubclassInterface() {
        assertTrue(TestableAnimal.class.isAssignableFrom(TestableDog.class));
        assertEquals("dog", ((TestableDog) new Dog("Woofy")).type$$());
    }

    @Test
    void shouldNotExposeStaticFields() {
        Set<String> methods = Arrays.stream(TestableDog.class.getMethods())
                                    .map(method -> method.getName())
                                    .collect(Collectors.toSet());

        assertFalse(methods.contains("staticDogField$$"));
        assertFalse(methods.contains("staticAnimalField$$"));
    }

    // Non-private methods are called like the code under test would: overrides apply
    @Test
    void shouldCallOverridingMethods() {
        assertEquals("...", ((TestableAnimal) new Animal("cat")).sound$());
        assertEquals("Woof", ((TestableDog) new Dog("Woofy")).sound$());
        assertEquals("Woof", ((TestableAnimal) new Dog("Woofy")).sound$());
    }

}
