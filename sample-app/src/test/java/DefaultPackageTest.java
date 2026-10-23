import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultPackageTest {

    @Test
    void shouldIgnoreClassesInTheDefaultPackage() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("TestableDefaultPackageClass"));
    }

}
