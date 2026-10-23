package net.mokatech.testable.sample.nested;

import net.mokatech.testable.api.Testable;

public class Right {

    /** Same simple name as Left.Node: the interfaces are named after the binary names (TestableRight$Node). */
    @Testable
    public static class Node {

        private String value = "right";
    }
}
