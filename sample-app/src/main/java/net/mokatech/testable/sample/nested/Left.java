package net.mokatech.testable.sample.nested;

import net.mokatech.testable.api.Testable;

public class Left {

    /** Same simple name as Right.Node: the interfaces are named after the binary names (TestableLeft$Node). */
    @Testable
    public static class Node {

        private int value = 1;
    }
}
