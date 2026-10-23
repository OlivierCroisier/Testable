package net.mokatech.testable.sample.visibility;

import net.mokatech.testable.api.Testable;
import net.mokatech.testable.sample.visibility.other.Hidden;

import java.util.List;

/** Fields whose type cannot be referenced from the generated interface are ignored. */
@Testable
public class Vault extends Hidden {

    // Not accessible from TestableVault: ignored
    private Secret secret = new Secret();
    private List<Secret> secrets = List.of(new Secret());
    private Secret[] secretArray = {new Secret()};
    private ProtectedNested protectedNested = new ProtectedNested();

    // Accessible from TestableVault: exposed
    private Hidden.PublicNested publicNested = new Hidden.PublicNested();
    private PackageNested packageNested = new PackageNested();

    private static class Secret {
    }

    static class PackageNested {
    }
}
