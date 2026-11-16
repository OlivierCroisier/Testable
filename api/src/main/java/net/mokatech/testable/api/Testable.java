package net.mokatech.testable.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes classes testable: a generated {@code Testable*} interface exposes their non-static fields and methods to the tests.
 * <ul>
 *     <li>On a package: all its top-level classes, records and enums are testable.</li>
 *     <li>On a class, a record or an enum (top-level or static nested): it is testable.</li>
 *     <li>On a non-static field, or the type of a non-static field (e.g. {@code List<@Testable User>}), of a testable class:
 *     the class of that type (or of its elements, for arrays) is made testable too.</li>
 *     <li>On a non-static method, or its return type, of a testable class: same, for the return type.</li>
 *     <li>On the superclass of a testable class, in its extends clause ({@code class Cat extends @Testable Feline}): the superclass is
 *     made testable too.</li>
 * </ul>
 * The interface of a testable class extends the interface of its direct superclass, if testable.
 * On a field, a method or the superclass of a class that is not testable, it has no effect but a warning. All other uses are compilation errors.
 * <p>
 * The annotation is not kept in the class files: classes from libraries can be made testable through a field or a method,
 * but their own annotations are not followed.
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE_USE})
public @interface Testable {
}
