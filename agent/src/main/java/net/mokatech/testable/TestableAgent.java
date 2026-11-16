package net.mokatech.testable;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.MethodList;
import net.bytebuddy.description.type.PackageDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.utility.JavaModule;
import net.mokatech.testable.api.TestableInterface;

import java.lang.instrument.Instrumentation;
import java.util.List;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.isSynthetic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class TestableAgent {

    /** Same conventions as the processor: foo$$() for the field foo, foo$(...) for the method foo. */
    private static final String FIELD_SUFFIX = "$$";
    private static final String METHOD_SUFFIX = "$";

    /** Same as the processor: classes of these packages are never testable. */
    private static final List<String> EXCLUDED_PACKAGES = List.of("java.", "javax.", "jdk.", "sun.", "com.sun.");

    /**
     * @param arguments "verbose" to log each transformed class
     */
    public static void premain(String arguments, Instrumentation instrumentation) {

        new AgentBuilder.Default()
                .with(new Listener("verbose".equals(arguments)))
                // Only keeps the classes whose TestableFoo interface exists: the others are not rewritten at all
                .type((typeDescription, classLoader, module, classBeingRedefined, protectionDomain) ->
                              isCandidate(typeDescription, classLoader) && findAccessorInterface(typeDescription, classLoader) != null)
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {

                    // Finds the TestableFoo interface generated for this class
                    TypeDescription accessorInterface = findAccessorInterface(typeDescription, classLoader);

                    DynamicType.Builder<?> currentBuilder = builder.implement(accessorInterface);
                    for (MethodDescription.InDefinedShape method : accessorInterface.getDeclaredMethods()) {

                        // foo$$() reads the field foo
                        if (method.getName().endsWith(FIELD_SUFFIX)) {
                            String fieldName = method.getName().substring(0, method.getName().length() - FIELD_SUFFIX.length());
                            // If it is missing, the interface is out of date: better fail here than with an AbstractMethodError in the test
                            if (typeDescription.getDeclaredFields().filter(named(fieldName).and(not(isStatic()))).isEmpty()) {
                                throw outOfDate("field " + fieldName, typeDescription, accessorInterface);
                            }
                            // Dynamic typing: the accessor may return the Testable interface of the field's class
                            currentBuilder = currentBuilder
                                    .method(is(method))
                                    .intercept(FieldAccessor.ofField(fieldName).withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
                            continue;
                        }

                        // foo$(...) delegates to the method foo with the same parameter types (after erasure)
                        String methodName = method.getName().substring(0, method.getName().length() - METHOD_SUFFIX.length());
                        MethodList<MethodDescription.InDefinedShape> targets = typeDescription.getDeclaredMethods()
                                .filter(named(methodName).and(not(isStatic())).and(not(isAbstract())).and(not(isSynthetic()))
                                                         .and(takesArguments(method.getParameters().asTypeList().asErasures())));
                        if (targets.size() != 1) {
                            throw outOfDate("method matching " + method.toGenericString(), typeDescription, accessorInterface);
                        }
                        currentBuilder = currentBuilder
                                .method(is(method))
                                .intercept(MethodCall.invoke(targets.getOnly())
                                                     .withAllArguments()
                                                     .withAssigner(Assigner.DEFAULT, Assigner.Typing.DYNAMIC));
                    }

                    return currentBuilder;
                })
                .installOn(instrumentation);
    }

    /**
     * Cheap pre-filter: @Testable is not kept in the class files, so a class is a candidate if the class file of its Testable interface exists.
     */
    private static boolean isCandidate(TypeDescription type, ClassLoader classLoader) {
        if (classLoader == null || type.isInterface() || type.isAnonymousType() || type.isLocalType() || type.isSynthetic()) {
            return false;
        }
        PackageDescription packageDescription = type.getPackage();
        if (packageDescription == null || packageDescription.getName().isEmpty()) {
            return false;
        }
        String packageName = packageDescription.getName() + ".";
        if (EXCLUDED_PACKAGES.stream().anyMatch(packageName::startsWith)) {
            return false;
        }
        return classLoader.getResource(interfaceName(type).replace('.', '/') + ".class") != null;
    }

    /**
     * Returns the interface generated for this class, or null if there is none (or if the interface with that name was not generated for it).
     */
    private static TypeDescription findAccessorInterface(TypeDescription type, ClassLoader classLoader) {
        TypePool.Resolution resolution = TypePool.Default.of(classLoader).describe(interfaceName(type));
        if (!resolution.isResolved()) {
            return null;
        }

        // Ignores an interface with the same name that was not generated for this class
        TypeDescription accessorInterface = resolution.resolve();
        AnnotationDescription.Loadable<TestableInterface> marker = accessorInterface.getDeclaredAnnotations().ofType(TestableInterface.class);
        if (marker == null || !marker.load().value().equals(type.getName())) {
            return null;
        }
        return accessorInterface;
    }

    /** Same convention as the processor: Testable + binary name without the package (Outer$Nested). */
    private static String interfaceName(TypeDescription type) {
        String packageName = type.getPackage().getName();
        return packageName + ".Testable" + type.getName().substring(packageName.length() + 1);
    }

    private static IllegalStateException outOfDate(String member, TypeDescription type, TypeDescription accessorInterface) {
        return new IllegalStateException("No " + member + " in " + type.getName() + ", " + accessorInterface.getName() + " is out of date: recompile the project");
    }

    /**
     * Resolves System.err on each event (and not when the agent is installed), to benefit from the redirection set up by Surefire.
     */
    private static class Listener extends AgentBuilder.Listener.Adapter {

        private final boolean verbose;

        Listener(boolean verbose) {
            this.verbose = verbose;
        }

        @Override
        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module, boolean loaded, DynamicType dynamicType) {
            if (verbose) {
                System.err.println("[Testable] Transformed " + typeDescription.getName());
            }
        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
            System.err.println("[Testable] Failed to transform " + typeName + ": " + throwable);
        }
    }

}
