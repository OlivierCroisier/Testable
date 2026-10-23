package net.mokatech.testable;

import net.mokatech.testable.api.Testable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.util.*;

@SupportedAnnotationTypes("net.mokatech.testable.api.Testable")
public class TestableAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        messager.printMessage(Diagnostic.Kind.NOTE, "TestableAnnotationProcessor.process()");

        Set<TypeElement> processedElements = new HashSet<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(Testable.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                messager.printMessage(Diagnostic.Kind.NOTE, "Found testable class : " + element);
                generateInterface((TypeElement) element);
                processedElements.add((TypeElement) element);
            } else if (element.getKind() == ElementKind.PACKAGE) {
                messager.printMessage(Diagnostic.Kind.NOTE, "Found testable package : " + element);
                element.getEnclosedElements().stream()
                       .filter(e -> e.getKind() == ElementKind.CLASS)
                       .peek(e -> messager.printMessage(Diagnostic.Kind.NOTE, "Found class in testable package : " + e))
                       .forEach(e -> {
                           if (processedElements.contains(e)) {
                               generateInterface((TypeElement) e);
                           }
                       });
            }
        }
        return true;
    }

    private void generateInterface(TypeElement classElement) {
        String packageName = processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();
        String className = classElement.getSimpleName().toString();
        String interfaceName = "Testable" + className;

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + interfaceName, classElement);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating interface for class: " + className + " in " + file.toUri());
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                if (!"".equals(packageName)) {
                    out.println("package " + packageName + ";");
                }
                out.println();
                out.println("public interface " + interfaceName + " {");

                for (Element enclosed : classElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.FIELD && !enclosed.getModifiers().contains(Modifier.STATIC)) {
                        String fieldName = enclosed.getSimpleName().toString();
                        String fieldType = enclosed.asType().toString();
                        out.println("    " + fieldType + " " + fieldName + "$();");
                    }
                }
                out.println("}");
            }
        } catch (Exception e) {
            processingEnv.getMessager()
                         .printMessage(Diagnostic.Kind.ERROR, "Error while generating the Testable interface " + interfaceName + ": " + e.getMessage(), classElement);
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
