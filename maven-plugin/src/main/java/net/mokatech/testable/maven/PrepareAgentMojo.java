package net.mokatech.testable.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.Map;
import java.util.Properties;

/**
 * Adds the Testable agent to the {@code argLine} property, which Surefire and Failsafe use by default to start the test JVM.
 * <p>
 * If the build configures its own {@code argLine}, it must reference this property with {@code @{argLine}}.
 */
@Mojo(name = "prepare-agent", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class PrepareAgentMojo extends AbstractMojo {

    private static final String AGENT_KEY = "net.mokatech.testable:agent";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** The plugin's own dependencies, which include the agent. */
    @Parameter(defaultValue = "${plugin.artifactMap}", readonly = true, required = true)
    private Map<String, Artifact> pluginArtifactMap;

    @Parameter(defaultValue = "argLine", required = true)
    private String propertyName;

    /** Makes the agent log each transformed class. */
    @Parameter(property = "testable.verbose", defaultValue = "false")
    private boolean verbose;

    @Parameter(property = "testable.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping Testable agent preparation");
            return;
        }

        Artifact agent = pluginArtifactMap.get(AGENT_KEY);
        if (agent == null || agent.getFile() == null) {
            throw new MojoExecutionException("Cannot find the Testable agent (" + AGENT_KEY + ") among the plugin's dependencies");
        }

        String option = "-javaagent:" + quoteIfNeeded(agent.getFile().getAbsolutePath()) + (verbose ? "=verbose" : "");
        Properties properties = project.getProperties();
        String previous = properties.getProperty(propertyName);
        String argLine = previous == null || previous.isBlank() ? option : previous + " " + option;
        properties.setProperty(propertyName, argLine);
        getLog().info(propertyName + " set to " + argLine);
    }

    /** Surefire splits the argLine on spaces: a path containing spaces (e.g. in the user's home) must be quoted. */
    private static String quoteIfNeeded(String path) {
        return path.contains(" ") ? '"' + path + '"' : path;
    }

}
