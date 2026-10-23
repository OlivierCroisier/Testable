package net.mokatech.testable.sample.methods;

import net.mokatech.testable.api.Testable;

/** Not testable itself: only its @Testable fields and methods are exposed. */
public class Partial {

    private String name = "partial";

    // Static: ignored, with a warning
    @Testable
    private static int instances;

    // Not private: ignored, with a warning
    @Testable
    String label = "label";

    @Testable
    private String greet(String who) {
        return "Hello " + who + " from " + name;
    }

    private String hidden() {
        return "hidden";
    }

    // Not private: ignored, with a warning
    @Testable
    String notPrivate() {
        return "not private";
    }
}
