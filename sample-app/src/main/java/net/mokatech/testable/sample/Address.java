package net.mokatech.testable.sample;

import lombok.Data;
import net.mokatech.testable.api.Testable;

/** Not testable itself: only its @Testable field is exposed. Also checks that Lombok still works alongside. */
@Data
public class Address {

    @Testable
    private String street;
    private String city;

}
