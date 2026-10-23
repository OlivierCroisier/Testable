package net.mokatech.testable.sample.enums;

import net.mokatech.testable.api.Testable;

@Testable
public enum Color {

    RED("#FF0000"),
    /** Constant with a body: an anonymous subclass of Color, which inherits the Testable implementation. */
    BLUE("#0000FF") {
        @Override
        public String toString() {
            return "blue";
        }
    };

    private final String hex;

    Color(String hex) {
        this.hex = hex;
    }
}
