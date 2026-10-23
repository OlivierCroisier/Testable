package net.mokatech.testable.sample.pkg;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackageTest {

    @Test
    void shouldExposeClassAnnotatedBothDirectlyAndThroughItsPackage() {
        assertEquals("annotated", ((TestableAnnotatedInPackage) new AnnotatedInPackage()).value$$());
    }

    @Test
    void shouldExposeClassAnnotatedThroughItsPackage() {
        assertEquals(42L, ((TestableNotAnnotatedInPackage) new NotAnnotatedInPackage()).value$$());
    }

    @Test
    void shouldExposeEnumAnnotatedThroughItsPackage() {
        assertEquals(7, ((TestablePackageEnum) (Object) PackageEnum.INSTANCE).counter$$());
    }

}
