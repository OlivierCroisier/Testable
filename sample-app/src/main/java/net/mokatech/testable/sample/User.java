package net.mokatech.testable.sample;

import net.mokatech.testable.api.Testable;

@Testable
public class User {

    private String secretName;
    private String password;

    public User(String name, String password) {
        this.password = password;
        this.secretName = name;
    }

}
