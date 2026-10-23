package net.mokatech.testable.maven;

import net.mokatech.testable.TestableAnnotationProcessor;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates the Testable interfaces from the main sources, and adds them to the test sources.
 * <p>
 * Runs javac in annotation-processing-only mode with the Testable processor alone, so the application's own processors and
 * its main compilation are left untouched, and the interfaces never reach {@code target/classes}.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** Where the interfaces are generated. Must not be {@code generated-test-sources/test-annotations}, which testCompile empties. */
    @Parameter(defaultValue = "${project.build.directory}/generated-test-sources/testable", required = true)
    private File outputDirectory;

    @Parameter(property = "maven.compiler.release")
    private String release;

    @Parameter(defaultValue = "${project.build.sourceEncoding}")
    private String encoding;

    @Parameter(property = "testable.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping Testable interfaces generation");
            return;
        }

        List<File> sources = findSources();
        if (sources.isEmpty()) {
            getLog().info("No sources to process");
            return;
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new MojoExecutionException("No Java compiler available: Maven must run on a JDK");
        }

        // Start from scratch, so that interfaces of deleted or no longer testable classes do not linger
        try {
            deleteRecursively(outputDirectory.toPath());
            Files.createDirectories(outputDirectory.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot prepare " + outputDirectory, e);
        }

        DiagnosticListener<JavaFileObject> listener = this::log;
        Charset charset = encoding == null ? null : Charset.forName(encoding);
        boolean success;
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(listener, Locale.getDefault(), charset)) {
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, listener, options(), null, fileManager.getJavaFileObjectsFromFiles(sources));
            // An explicit processor disables discovery: the application's own processors (Lombok...) do not run again
            task.setProcessors(List.of(new TestableAnnotationProcessor()));
            success = task.call();
        } catch (IOException e) {
            throw new MojoExecutionException("Error while generating the Testable interfaces", e);
        }
        if (!success) {
            throw new MojoFailureException("Testable interfaces generation failed, see the errors above");
        }

        project.addTestCompileSourceRoot(outputDirectory.getAbsolutePath());
    }

    private List<File> findSources() throws MojoExecutionException {
        List<File> sources = new ArrayList<>();
        for (String root : project.getCompileSourceRoots()) {
            Path rootPath = Path.of(root);
            if (!Files.isDirectory(rootPath)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(rootPath)) {
                files.filter(file -> file.toString().endsWith(".java"))
                     .map(Path::toFile)
                     .forEach(sources::add);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot list the sources of " + root, e);
            }
        }
        return sources;
    }

    private List<String> options() throws MojoExecutionException {
        List<String> options = new ArrayList<>(List.of("-proc:only", "-s", outputDirectory.getAbsolutePath()));
        try {
            // Includes target/classes, so that already compiled main classes resolve too
            options.add("-classpath");
            options.add(String.join(File.pathSeparator, project.getCompileClasspathElements()));
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot resolve the compile classpath", e);
        }
        if (release != null && !release.isBlank()) {
            options.add("--release");
            options.add(release);
        }
        if (encoding != null) {
            options.add("-encoding");
            options.add(encoding);
        }
        return options;
    }

    private void log(Diagnostic<? extends JavaFileObject> diagnostic) {
        String location = diagnostic.getSource() == null ? "" : diagnostic.getSource().getName() + ":[" + diagnostic.getLineNumber() + "," + diagnostic.getColumnNumber() + "] ";
        String message = location + diagnostic.getMessage(Locale.getDefault());
        switch (diagnostic.getKind()) {
            case ERROR:
                getLog().error(message);
                break;
            case WARNING:
            case MANDATORY_WARNING:
                getLog().warn(message);
                break;
            default:
                getLog().info(message);
        }
    }

    private static void deleteRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Files.delete(path);
            }
        }
    }

}
