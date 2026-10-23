package net.mokatech.testable.sample.visibility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VaultTest {

    @Test
    void shouldExposeAccessibleFieldTypes() {
        TestableVault testableVault = (TestableVault) new Vault();
        assertNotNull(testableVault.publicNested$());
        assertNotNull(testableVault.packageNested$());
    }

    @Test
    void shouldIgnoreInaccessibleFieldTypes() {
        assertThrows(NoSuchMethodException.class, () -> TestableVault.class.getMethod("secret$"));
        assertThrows(NoSuchMethodException.class, () -> TestableVault.class.getMethod("secrets$"));
        assertThrows(NoSuchMethodException.class, () -> TestableVault.class.getMethod("secretArray$"));
        assertThrows(NoSuchMethodException.class, () -> TestableVault.class.getMethod("protectedNested$"));
    }

}
