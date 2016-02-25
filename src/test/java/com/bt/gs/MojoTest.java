package com.bt.gs;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.LegacySupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.plugin.testing.stubs.ArtifactStub;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.sonatype.aether.impl.internal.SimpleLocalRepositoryManager;
import org.sonatype.aether.util.DefaultRepositorySystemSession;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Faizal Sidek
 *         Created on 2/20/2016
 */
public class MojoTest extends AbstractMojoTestCase {
    public void testLoadPom() throws Exception {
        File testPom = new File(getBasedir(),
                "src/test/resources/unit/basic-test/project-config.xml");

        /*
        IvyGen mojo = (IvyGen) lookupMojo("generate", testPom);
        setVariableValueToObject(mojo, "project", getMockMavenProject(testPom));


        LegacySupport legacySupport = lookup(LegacySupport.class);
        legacySupport.setSession(newMavenSession(new MavenProjectStub()));
        setVariableValueToObject(mojo, "session", legacySupport.getSession());

        mojo.setJarArchiver(new JarArchiver());

        mojo.execute();
        */
    }

    private MavenProject getMockMavenProject(File pomFile) {
        Model model = null;
        FileReader reader = null;
        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        try {
            reader = new FileReader(pomFile);
            model = mavenreader.read(reader);
            model.setPomFile(pomFile);
        } catch (Exception ex) {
        }
        MavenProject project = new MavenProject(model);

        return project;
    }
}
