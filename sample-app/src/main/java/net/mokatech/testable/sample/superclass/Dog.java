package net.mokatech.testable.sample.superclass;

import net.mokatech.testable.api.Testable;

@Testable
public class Dog extends Animal {

    public static String staticDogField;

    private String name;

    public Dog(String name) {
        super("dog");
        this.name = name;
    }
}
