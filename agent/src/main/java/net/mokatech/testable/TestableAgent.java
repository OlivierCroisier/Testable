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
import net.mokatech.testable.api.Testable;
import net.mokatech.testable.api.TestableInterface;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class TestableAgent {

    /** Mêmes conventions que le processeur : foo$$() pour le champ foo, foo$(...) pour la méthode privée foo. */
    private static final String FIELD_SUFFIX = "$$";
    private static final String METHOD_SUFFIX = "$";

    /**
     * @param arguments "verbose" pour tracer chaque classe transformée
     */
    public static void premain(String arguments, Instrumentation instrumentation) {

        new AgentBuilder.Default()
                .with(new Listener("verbose".equals(arguments)))
                // Ne retient que les classes dont l'interface TestableFoo existe : les autres ne sont pas réécrites du tout
                .type((typeDescription, classLoader, module, classBeingRedefined, protectionDomain) ->
                              isTestable(typeDescription) && findAccessorInterface(typeDescription, classLoader) != null)
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {

                    // Trouve l'interface TestableFoo générée pour cette classe
                    TypeDescription accessorInterface = findAccessorInterface(typeDescription, classLoader);

                    DynamicType.Builder<?> currentBuilder = builder.implement(accessorInterface);
                    for (MethodDescription.InDefinedShape method : accessorInterface.getDeclaredMethods()) {

                        // foo$$() lit le champ foo
                        if (method.getName().endsWith(FIELD_SUFFIX)) {
                            String fieldName = method.getName().substring(0, method.getName().length() - FIELD_SUFFIX.length());
                            // S'il manque, l'interface est périmée : mieux vaut échouer ici qu'avec une AbstractMethodError dans le test
                            if (typeDescription.getDeclaredFields().filter(named(fieldName).and(isPrivate()).and(not(isStatic()))).isEmpty()) {
                                throw outOfDate("field " + fieldName, typeDescription, accessorInterface);
                            }
                            currentBuilder = currentBuilder
                                    .method(is(method))
                                    .intercept(FieldAccessor.ofField(fieldName));
                            continue;
                        }

                        // foo$(...) délègue à la méthode privée foo de mêmes types de paramètres (après effacement)
                        String methodName = method.getName().substring(0, method.getName().length() - METHOD_SUFFIX.length());
                        MethodList<MethodDescription.InDefinedShape> targets = typeDescription.getDeclaredMethods()
                                .filter(named(methodName).and(isPrivate()).and(not(isStatic()))
                                                         .and(takesArguments(method.getParameters().asTypeList().asErasures())));
                        if (targets.size() != 1) {
                            throw outOfDate("private method matching " + method.toGenericString(), typeDescription, accessorInterface);
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
     * Mêmes candidats que le processeur : classes et enums annotés ou ayant un champ ou une méthode annotés, ou classes et enums de premier niveau d'un package annoté.
     */
    private static boolean isTestable(TypeDescription type) {
        if (type.isInterface() || type.isAnonymousType() || type.isLocalType()) {
            return false;
        }
        if (type.getDeclaredAnnotations().isAnnotationPresent(Testable.class)
            || !type.getDeclaredFields().filter(isAnnotatedWith(Testable.class)).isEmpty()
            || !type.getDeclaredMethods().filter(isAnnotatedWith(Testable.class)).isEmpty()) {
            return true;
        }
        PackageDescription packageDescription = type.getPackage();
        return !type.isMemberType()
               && packageDescription != null
               && packageDescription.getDeclaredAnnotations().isAnnotationPresent(Testable.class);
    }

    /**
     * Renvoie l'interface générée pour cette classe, ou null s'il n'y en a pas (package par défaut, classe de test dans un package annoté...).
     */
    private static TypeDescription findAccessorInterface(TypeDescription type, ClassLoader classLoader) {
        PackageDescription packageDescription = type.getPackage();
        if (packageDescription == null || packageDescription.getName().isEmpty()) {
            return null;
        }

        // Même convention que le processeur : Testable + nom binaire sans le package (Outer$Inner)
        String packageName = packageDescription.getName();
        String interfaceName = packageName + ".Testable" + type.getName().substring(packageName.length() + 1);
        TypePool.Resolution resolution = TypePool.Default.of(classLoader).describe(interfaceName);
        if (!resolution.isResolved()) {
            return null;
        }

        // Ignore une interface homonyme qui n'aurait pas été générée pour cette classe
        TypeDescription accessorInterface = resolution.resolve();
        AnnotationDescription.Loadable<TestableInterface> marker = accessorInterface.getDeclaredAnnotations().ofType(TestableInterface.class);
        if (marker == null || !marker.load().value().equals(type.getName())) {
            return null;
        }
        return accessorInterface;
    }

    private static IllegalStateException outOfDate(String member, TypeDescription type, TypeDescription accessorInterface) {
        return new IllegalStateException("No " + member + " in " + type.getName() + ", " + accessorInterface.getName() + " is out of date: recompile the project");
    }

    /**
     * Résout System.err à chaque événement (et non à l'installation de l'agent), pour profiter de la redirection mise en place par Surefire.
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
