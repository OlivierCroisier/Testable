package net.mokatech.testable.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes private members to the tests, through a generated {@code Testable*} interface.
 * <ul>
 *     <li>On a class or an enum: all its private non-static fields and private non-static methods.</li>
 *     <li>On a package: the same, for all its top-level classes and enums.</li>
 *     <li>On a private non-static field or a private non-static method: only that member.</li>
 * </ul>
 * <p>
 * Can be used as a direct replacement of the following {@code @VisibleForTesting} annotations :
 * <ul>
 *     <li>Guava: com.google.common.annotations.VisibleForTesting</li>
 *     <li>AssertJ: org.assertj.core.util.VisibleForTesting</li>
 *     <li>Apache Flink: org.apache.flink.annotation.VisibleForTesting</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
public @interface Testable {
}
