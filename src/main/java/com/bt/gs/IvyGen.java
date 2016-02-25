package com.bt.gs;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Faizal Sidek
 *         Created on 2/20/2016
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class IvyGen extends AbstractMojo {
    public final Log log = getLog();

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}/buildscript", property = "buildscriptDir", required = true)
    private File buildscriptDir;

    @Parameter(defaultValue = "${project.build.directory}/components", property = "componentsDir", required = true)
    private File componentDir;

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    @Parameter(property = "scope")
    private String scope;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Executing ivy gen...");

        try {
            ArtifactFilter artifactFilter = createResolvingArtifactFilter();
            ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
            projectBuildingRequest.setProject(project);

            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(projectBuildingRequest);
            DependencyNode dependencyNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, artifactFilter);

            Artifact dependency = dependencyNode.getArtifact();
            log.debug("***************************");
            log.debug("GroupId=" + dependency.getGroupId());
            log.debug("ArtifactId=" + dependency.getArtifactId());
            log.debug("Version=" + dependency.getVersion());
            log.debug("Scope=" + dependency.getScope());
            log.debug("File=" + dependency.getFile().getAbsolutePath());
            log.debug("***************************");

            for (DependencyNode children : dependencyNode.getChildren()) {
                Artifact artifact = children.getArtifact();
                log.debug("\t***************************");
                log.debug("\tGroupId=" + artifact.getGroupId());
                log.debug("\tArtifactId=" + artifact.getArtifactId());
                log.debug("\tVersion=" + artifact.getVersion());
                log.debug("\tScope=" + artifact.getScope());
                log.debug("\tFile=" + artifact.getFile().getAbsolutePath());
                log.debug("\t***************************");
                if (children.getChildren() != null && children.getChildren().size() > 0) {
                    for (DependencyNode node : children.getChildren()) {
                        log.debug("\t\t***************************");
                        log.debug("\t\tGroupId=" + node.getArtifact().getGroupId());
                        log.debug("\t\tArtifactId=" + node.getArtifact().getArtifactId());
                        log.debug("\t\tVersion=" + node.getArtifact().getVersion());
                        log.debug("\t\tScope=" + node.getArtifact().getScope());
                        log.debug("\t\tFile=" + node.getArtifact().getFile().getAbsolutePath());
                        log.debug("\t\t***************************");
                    }
                }
            }
        } catch (DependencyGraphBuilderException e) {
            log.error("Unable to build dependencies graph: " + e.getMessage(), e);
        }


        DependencyManagement management = project.getDependencyManagement();
        if (management != null) {
            for (Dependency dependency : management.getDependencies()) {
                log.debug("***************************");
                log.debug("GroupId=" + dependency.getGroupId());
                log.debug("ArtifactId=" + dependency.getArtifactId());
                log.debug("Version=" + dependency.getVersion());
                log.debug("Scope=" + dependency.getScope());
                log.debug("***************************");
            }
        }

        DependencyNode dependencyNode = null;
        try {
            ArtifactFilter artifactFilter = createResolvingArtifactFilter();
            ProjectBuildingRequest projectBuildingRequest = session.getProjectBuildingRequest();
            projectBuildingRequest.setProject(project);

            ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(projectBuildingRequest);
            dependencyNode = dependencyGraphBuilder.buildDependencyGraph(buildingRequest, artifactFilter);
        } catch (DependencyGraphBuilderException e) {
            log.error("Unable to build dependency graph: " + e.getMessage(), e);
        }

        for (Artifact artifact : getAllDependencies(dependencyNode)) {
            String targetFileName = getTargetFileName(artifact);
            String targetPath = getTargetPath(artifact);

            log.debug("Creating directory " + (targetPath + File.separator + "jars" + File.separator));
            File destination = new File(targetPath + File.separator + "jars" + File.separator);
            if (!destination.exists())
                assert destination.mkdirs();

            try {
                log.debug("Copying " + artifact.getFile().getAbsolutePath() + " to " + (targetPath + File.separator + "jars" + File.separator + targetFileName));
                FileUtils.copyFile(artifact.getFile(), new File(targetPath + File.separator + "jars" + File.separator + targetFileName));

                log.debug("Creating ivy xml on dir " + (targetPath + File.separator));
                IvyXml xml = new IvyXml(artifact);
                xml.setLog(log);
                PrintWriter printWriter = new PrintWriter(new File(targetPath + File.separator + "ivy.xml"));
                printWriter.write(xml.generateIvyXmlForArtifact());
                printWriter.close();
            } catch (Exception e) {
                log.error("Unable to create file: " + e.getMessage(), e);
            }
        }

        try {
            log.debug("Creating master ivy.xml");
            if (!buildscriptDir.exists())
                assert buildscriptDir.mkdirs();

            File xmlFile = new File(buildscriptDir.getAbsolutePath() + File.separator + "ivy.xml");
            assert xmlFile.createNewFile();
            PrintWriter writer = new PrintWriter(xmlFile);
            IvyXml xml = new IvyXml(project, getAllDependencies(dependencyNode));
            xml.setLog(log);
            writer.print(xml.generateIvy());
            writer.close();
        } catch (Exception e) {
            log.error("Unable to create ivy xml: " + e.getMessage(), e);
        }
    }

    private Set<Artifact> getAllDependencies(DependencyNode dependencyNode) {
        Set<Artifact> artifacts = new HashSet<>();

        for (DependencyNode children : dependencyNode.getChildren()) {
            artifacts.add(children.getArtifact());
            if (children.getChildren() != null && children.getChildren().size() > 0)
                artifacts.addAll(getAllDependencies(children));
        }

        return artifacts;
    }

    private ArtifactFilter createResolvingArtifactFilter() {
        ArtifactFilter filter;

        if (scope != null) {
            log.debug("+ Resolving dependency tree for scope '" + scope + "'");
            filter = new ScopeArtifactFilter(scope);
        } else {
            filter = null;
        }

        return filter;
    }

    private String getTargetFileName(Artifact artifact) {
        return artifact.getArtifactId() + "-" + artifact.getVersion() + ".jar";
    }

    private String getTargetPath(Artifact artifact) {
        return componentDir + File.separator + artifact.getArtifactId() + File.separator + artifact.getArtifactId() + "-" + artifact.getVersion();
    }
}
