# Testable

Testable lets unit tests read the **private fields** and call the **private methods** of a class without changing the class itself, without reflection in the test code, and without touching production artifacts.

```java
@Testable
public class User {
    private String secretName;
    private String password;

    private boolean checkPassword(String candidate) {
        return password.equals(candidate);
    }
}

// In a test
TestableUser testableUser = (TestableUser) new User("James Bond", "password007");
assertEquals("James Bond", testableUser.secretName$$());
assertTrue(testableUser.checkPassword$("password007"));
```

The test code is fully typed: renaming a member or changing a signature causes compile errors in the tests, not runtime reflection failures.

## How it works

Testable has two parts:

1. **At compile time**, an annotation processor generates a `Testable<ClassName>` interface for each testable class or enum. The interface declares one getter-like method per field, and one mirror method per private method.
2. **At test runtime**, a Java agent rewrites each testable class as it is loaded so that it implements its generated interface. ByteBuddy generates the method bodies: each one returns the value of its field, or calls its private method.

The tests cast the object to the generated interface and call its methods.

```mermaid
flowchart LR
    subgraph COMPILE["Compile time (testable:generate)"]
        direction TB
        SRC["src/main/java<br/><code>@Testable class User</code><br/><code>private String password;</code>"]
        AP["TestableAnnotationProcessor<br/>(processor module)"]
        GEN["target/generated-test-sources<br/><code>interface TestableUser {</code><br/><code>  String password$$();</code><br/><code>}</code>"]
        SRC -- "scanned by" --> AP
        AP -- "generates" --> GEN
    end

    subgraph TESTCOMPILE["Test compile"]
        direction TB
        TEST["src/test/java<br/><code>((TestableUser) user).password$$()</code>"]
    end

    subgraph RUNTIME["Test runtime (-javaagent set by testable:prepare-agent)"]
        direction TB
        CL["Class loading of <code>User</code>"]
        AG["TestableAgent<br/>(agent module, ByteBuddy)"]
        TX["Transformed <code>User</code><br/><code>implements TestableUser</code><br/><code>password$$() { return this.password; }</code>"]
        CL -- "intercepted by" --> AG
        AG -- "redefines" --> TX
    end

    GEN -- "compiled with" --> TEST
    GEN -. "looked up by name" .-> AG
    TEST -- "cast + call" --> TX
```

### 1. Marking members as testable

The `@Testable` annotation (`api` module) can be placed on:

- **a class or an enum**: that type becomes fully testable, exposing its private non-static fields and methods. Nested types must be annotated separately.
- **a package** (in `package-info.java`): every top-level class and enum of that package becomes fully testable.
- **a private field**: only that field is exposed, even if its class isn't testable. `@Testable` on a non-private or static field is ignored with a `WARNING`.
- **a private method**: only that method is exposed, even if its class isn't testable. `@Testable` on a non-private or static method is ignored with a `WARNING`.

Only **private, non-static** members are exposed. Non-private members are already accessible to tests in the same package, so they don't need an accessor.

When a class isn't fully testable, its interface contains only its annotated fields and methods:

```java
@Data
public class Address {
    @Testable
    private String street;
    private String city;
}

// Generated: only street is exposed
public interface TestableAddress {
    java.lang.String street$$();
}
```

A type annotated directly and also through its package gets only one interface. Classes in the default package are ignored, and the processor reports them with a `WARNING`.

### 2. Generating the interfaces (processor)

For each testable type, `TestableAnnotationProcessor` generates a public interface in the same package. The interface is named `Testable` followed by the type's binary name without the package, so nested types sharing a simple name never collide:

| Testable type      | Generated interface   |
|--------------------|-----------------------|
| `com.acme.Foo`     | `com.acme.TestableFoo`        |
| `com.acme.Box.Inner` | `com.acme.TestableBox$Inner` |
| `com.acme.Left.Node` / `com.acme.Right.Node` | `com.acme.TestableLeft$Node` / `com.acme.TestableRight$Node` |

The interface is annotated with `@TestableInterface("<binary name of the type>")`, which tells the agent which type it was generated for.

Each **private, non-static field declared by the type itself** becomes a method named `<field>$$()` that returns the field's type. The double `$` keeps field accessors apart from private methods, which use a single `$`:

| Field in `Foo`                    | Method in `TestableFoo`                                          |
|-----------------------------------|------------------------------------------------------------------|
| `private String name;`            | `java.lang.String name$$();`                                      |
| `private int[][] grid;`           | `int[][] grid$$();`                                               |
| `private List<? extends U> nums;` | `java.util.List<? extends U> nums$$();`                           |
| `private T outerValue;` (in an inner class, `T` from the outer class) | `java.lang.Comparable outerValue$$();` (erased) |

Generic classes keep their type parameters and bounds: `Box<T extends Comparable<T>, U, V>` becomes `TestableBox<T extends Comparable<T>, U, V>`.

**Ignored fields:**

- Non-private fields, which tests in the same package can read directly, and `static` fields.
- Fields inherited from superclasses. Annotate the superclass to test it.
- Fields whose type cannot be referenced from the generated interface, such as a `private` nested class, a local or anonymous class, or a non-public type from another package. The processor reports these with a `NOTE`.
- Fields whose type cannot be resolved. If another annotation processor might still generate the type, the class is deferred to a later round. If the type is still unresolved in the last round, the field is dropped with a `WARNING`.

#### Private methods

Each **private, non-static method declared by the type itself** becomes a method with the same name followed by `$`, the same parameters (names included), and the same `throws` clause:

| Method in `Foo`                                                   | Method in `TestableFoo`                                                    |
|-------------------------------------------------------------------|----------------------------------------------------------------------------|
| `private int add(int a, int b)`                                   | `int add$(int a, int b);`                                                  |
| `private String format(int v)` / `private String format(String v)` | `java.lang.String format$(int v);` / `java.lang.String format$(java.lang.String v);` |
| `private <R extends Comparable<R>> R max(List<R> values)`         | `<R extends java.lang.Comparable<R>> R max$(java.util.List<R> values);`   |
| `private int sum(int... values)`                                  | `int sum$(int... values);`                                                 |
| `private int parse(String s) throws IOException`                  | `int parse$(java.lang.String s) throws java.io.IOException;`              |
| `private void fail() throws SecretException` (private exception)  | `void fail$() throws java.lang.Exception;` (closest accessible superclass) |
| `private T orDefault(T other)` (in an inner class, `T` from the outer class) | `java.lang.Comparable orDefault$(java.lang.Comparable other);` (erased) |

Exceptions thrown by the private method propagate unchanged, with no `InvocationTargetException` wrapping.

**Ignored methods:**

- Non-private and `static` methods, methods inherited from superclasses, and methods declared in enum constant bodies.
- Methods whose name ends with `$`, such as `legacy$()`. They would become `legacy$$()`, which reads as the accessor of a field `legacy`. They are reported with a `WARNING`.
- Methods whose return or parameter types cannot be referenced from the generated interface (same rules as fields), reported with a `NOTE`.
- Methods with a parameter typed by a type variable whose bounds cannot be expressed in the interface, such as `<K extends PrivateKey> void take(K key)`. The interface's parameter types would erase differently from the class's, and the agent finds the private method by its erased parameter types. These are reported with a `NOTE`.
- Methods whose signature cannot be resolved, handled like fields (deferred, then dropped with a `WARNING`).

### 3. Implementing the interfaces (agent)

`TestableAgent` is a `premain` Java agent built on ByteBuddy and shaded into a self-contained jar. It considers the same candidates as the processor: classes and enums annotated with `@Testable` or declaring a `@Testable` field or method, and top-level classes and enums of `@Testable` packages. For each candidate that is loaded, the agent:

1. derives the interface name using the processor's naming rule,
2. resolves that interface from the class's class loader,
3. skips the class entirely if the interface does not exist, or if its `@TestableInterface` marker names a different class. This is the case for test classes that live in a `@Testable` package,
4. makes the class implement the interface,
5. implements each interface method:
   - `<name>$$()` becomes a direct read of the private non-static field `<name>` (`FieldAccessor`),
   - `<name>$(...)` calls the private non-static method `<name>` with the same erased parameter types (`MethodCall`).

If an interface method has no matching field or private method, the interface is out of date (for example, the class changed but the processor didn't run again). In that case the class is left untransformed, and the agent prints the reason on `System.err`:

```
[Testable] Failed to transform com.acme.User: java.lang.IllegalStateException: No private method matching public abstract int com.acme.TestableUser.ghost$(int) in com.acme.User, com.acme.TestableUser is out of date: recompile the project
```

Pass `verbose` as the agent argument (`-javaagent:testable-agent.jar=verbose`, or `-Dtestable.verbose` with the Maven plugin) to also log each transformed class.

The interface must be added **before the class is loaded**, because the JVM does not let a retransformation change a class's hierarchy. Attaching the agent with `-javaagent` guarantees that it is active before any class is loaded. It does not support dynamic attachment.

## Project layout

| Module         | Role                                                                                         | Used by the application in |
|----------------|----------------------------------------------------------------------------------------------|----------------------------|
| `api`          | The `@Testable` annotation, and the `@TestableInterface` marker put on generated interfaces | main code (compile scope)  |
| `processor`    | `TestableAnnotationProcessor`: generates the `Testable*` interfaces                          | the Maven plugin           |
| `agent`        | `TestableAgent`: ByteBuddy agent, shaded jar with `Premain-Class`                            | the Maven plugin           |
| `maven-plugin` | `testable-maven-plugin`: runs the processor and attaches the agent to the tests              | the build                  |
| `sample-app`   | Standalone Maven project that demonstrates and tests the whole chain                         | n/a                        |

## Using it in a Maven project

Declare the `api` dependency, and add the `testable-maven-plugin` with its two goals:

```xml
<dependencies>
    <dependency>
        <groupId>net.mokatech.testable</groupId>
        <artifactId>api</artifactId>
        <version>${lib.testable.version}</version>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>net.mokatech.testable</groupId>
            <artifactId>testable-maven-plugin</artifactId>
            <version>${lib.testable.version}</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                        <goal>prepare-agent</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

The rest of the build, including the application's own annotation processors such as Lombok, stays unchanged.

### `generate` goal

The `generate` goal runs in `generate-test-sources` by default. It runs javac over the main sources with `-proc:only` and the Testable processor alone, using the project's compile classpath, `maven.compiler.release` and source encoding. The interfaces are written to `target/generated-test-sources/testable` and added to the test sources. They never reach `target/classes` or the production jar.

The output directory is emptied first, so interfaces of deleted classes do not linger. It must not be `generated-test-sources/test-annotations`, because `testCompile` empties that directory before compiling.

Maven must run on a JDK, not a JRE.

### `prepare-agent` goal

The `prepare-agent` goal runs in `initialize` by default. It takes the agent jar from the plugin's own dependencies and appends `-javaagent:<agent jar>` to the `argLine` property, which Surefire and Failsafe use by default to start the test JVM. The application doesn't need to declare the agent.

If the build configures its own `argLine`, for example for JaCoCo, that `argLine` must include the property: `<argLine>@{argLine} -Xmx1g</argLine>`. Otherwise it replaces the agent option, and every cast to a `Testable*` interface fails.

### Options

| Parameter         | User property      | Default                                                | Goal            | Description                                   |
|-------------------|--------------------|--------------------------------------------------------|-----------------|-----------------------------------------------|
| `skip`            | `testable.skip`    | `false`                                                | both            | Skips the goal                                |
| `outputDirectory` |                    | `${project.build.directory}/generated-test-sources/testable` | `generate`      | Where the interfaces are generated            |
| `verbose`         | `testable.verbose` | `false`                                                | `prepare-agent` | Makes the agent log each transformed class    |
| `propertyName`    |                    | `argLine`                                              | `prepare-agent` | Property that receives the `-javaagent` option |

### Without the plugin

`sample-app/pom-noplugin.xml` shows the same setup with standard plugins only:
- a second `maven-compiler-plugin` execution for the processor,
- `build-helper-maven-plugin` for the test source root,
- `maven-dependency-plugin`'s `properties` goal for the agent jar's path,
- a surefire `argLine`.

## Building

Testable requires Java 17 or later.

```shell
# 1. Build and install the library modules (api, processor, agent, maven-plugin)
mvn install

# 2. Build and test the sample application
cd sample-app
mvn test
```

## Limitations

- Classes in the default package are not supported.
- Only classes and enums are supported. Records and interfaces are not.
- Enums and `final` classes must be cast through `Object`, as in `(TestableColor) (Object) Color.RED`. Otherwise javac rejects the cast, because it knows that such a type cannot implement the interface.
- Superclass fields and methods are not exposed through the subclass's interface. Make the superclass `@Testable` instead.
- Private static methods are not exposed.
- When a testable class and its testable superclass both declare a private member with the same name and signature, calling the superclass's interface method on a subclass instance reaches the subclass's member. Both interfaces declare the same method, so the subclass's implementation overrides the superclass's.
- If the agent is missing from the test JVM, every cast to a `Testable*` interface fails with a `ClassCastException`.
