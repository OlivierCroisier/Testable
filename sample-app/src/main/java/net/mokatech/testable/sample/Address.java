package net.mokatech.testable.sample;

import lombok.Data;

/** Made testable through the @Testable field of User. Also checks that Lombok still works alongside. */
@Data
public class Address {

    private String street;
    private String city;

}
