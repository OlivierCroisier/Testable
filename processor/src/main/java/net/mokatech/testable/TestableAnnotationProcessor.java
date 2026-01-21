package net.mokatech.testable;

import net.mokatech.testable.api.Testable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.util.Set;

@SupportedAnnotationTypes("net.mokatech.testable.api.Testable")
@SupportedSourceVersion(SourceVersion.RELEASE_11)
public class TestableAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "TestableAnnotationProcessor.process()");
        for (Element element : roundEnv.getElementsAnnotatedWith(Testable.class)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found annotated class: " + element.toString());
            if (element.getKind() == ElementKind.CLASS) {
                generateInterface((TypeElement) element);
            }
        }
        return true;
    }

    private void generateInterface(TypeElement classElement) {
        String packageName = processingEnv.getElementUtils().getPackageOf(classElement).getQualifiedName().toString();
        String className = classElement.getSimpleName().toString();
        String interfaceName = "Testable" + className;


        try {
            ModuleElement module = processingEnv.getElementUtils().getModuleOf(classElement);
            // FileObject file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, packageName, interfaceName + ".java", classElement);
            JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + interfaceName);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generating interface for class: " + className + " in " + file.toUri());
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.println("package " + packageName + ";");
                out.println();
                out.println("public interface " + interfaceName + " {");

                for (Element enclosed : classElement.getEnclosedElements()) {
                    if (enclosed.getKind() == ElementKind.FIELD) {
                        String fieldName = enclosed.getSimpleName().toString();
                        String fieldType = enclosed.asType().toString();
                        out.println("    " + fieldType + " " + fieldName + "$();");
                    }
                }
                out.println("}");
            }
        } catch (Exception e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error while generating the Testable interface " + interfaceName + ": " + e.getMessage(), classElement);
        }
    }
}
