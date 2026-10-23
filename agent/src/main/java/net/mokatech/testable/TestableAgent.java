package net.mokatech.testable;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.pool.TypePool;
import net.mokatech.testable.api.Testable;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;

public class TestableAgent {

    public static void premain(String arguments, Instrumentation instrumentation) {

        new AgentBuilder.Default()
                .type(getOr())
                .transform((builder, typeDescription, classLoader, module, protectionDomain) -> {

                    System.out.println("Transforming class " + typeDescription.getName());
                    String interfaceName = typeDescription.getPackage().getActualName() + ".Testable" + typeDescription.getSimpleName();

                    try {
                        // Trouve l'interface TestableFoo
                        TypePool typePool = TypePool.Default.of(classLoader);
                        TypeDescription accessorInterface = typePool.describe(interfaceName).resolve();

                        DynamicType.Builder<?> currentBuilder = builder.implement(accessorInterface);
                        for (MethodDescription.InDefinedShape method : accessorInterface.getDeclaredMethods()) {

                            // Trouve le champ correspondant
                            String fieldName = deriveFieldName(method.getName());
                            if (typeDescription.getDeclaredFields().filter(ElementMatchers.named(fieldName)).isEmpty()) {
                                continue;
                            }

                            // Implémente la méthode pour qu'elle lise le champ
                            currentBuilder = currentBuilder
                                    .method(ElementMatchers.is(method))
                                    .intercept(FieldAccessor.ofField(fieldName));
                        }

                        return currentBuilder;
                    } catch (Exception e) {
                        System.err.println("Interface not found: " + e.getMessage());
                        return builder;
                    }
                })
                .installOn(instrumentation);
    }

    private static ElementMatcher.Junction<TypeDescription> getOr() {
        return isAnnotatedWith(Testable.class)
                .or(td -> td.getPackage() != null && td.getPackage().getDeclaredAnnotations().isAnnotationPresent(Testable.class));
    }

    private static String deriveFieldName(String methodName) {
        if (methodName.endsWith("$")) {
            return methodName.substring(0, methodName.length() - 1);
        }
        return methodName;
    }

}
