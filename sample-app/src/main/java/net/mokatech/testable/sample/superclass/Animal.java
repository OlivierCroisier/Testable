package net.mokatech.testable.sample.superclass;

import net.mokatech.testable.api.Testable;

@Testable
public class Animal {

    public static String staticAnimalField;

    private String type;

    public Animal(String type) {
        this.type = type;
    }

    protected String sound() {
        return "...";
    }
}
