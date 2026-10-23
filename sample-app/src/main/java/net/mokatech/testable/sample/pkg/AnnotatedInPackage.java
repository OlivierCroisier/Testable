package net.mokatech.testable.sample.pkg;

import net.mokatech.testable.api.Testable;

/** Testable both directly and through its package: the interface must be generated only once. */
@Testable
public class AnnotatedInPackage {

    private String value = "annotated";
}
