package net.mokatech.testable.sample;

import net.mokatech.testable.api.Testable;

@Testable
public class User {

    private String secretName;
    private String password;

    /** Address is not testable itself: the annotation makes it testable, and address$$() returns a TestableAddress. */
    @Testable
    private Address address;

    public User(String name, String password, Address address) {
        this.password = password;
        this.secretName = name;
        this.address = address;
    }

}
