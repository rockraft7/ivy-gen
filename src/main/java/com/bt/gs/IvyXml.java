package com.bt.gs;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author Faizal Sidek
 *         Created on 2/22/2016
 */
public class IvyXml {
    private Log log;
    private MavenProject mavenProject;
    private Artifact artifact;
    private DocumentBuilder docBuilder;
    private Set<Artifact> artifacts;
    private Document doc;

    public IvyXml(MavenProject mavenProject, Set<Artifact> artifacts) {
        this.mavenProject = mavenProject;
        this.artifacts = artifacts;
        initDocBuilder();
    }

    public IvyXml(Artifact artifact) {
        this.artifact = artifact;
        initDocBuilder();
    }

    public void setLog(Log log) {
        this.log = log;
    }

    private void initDocBuilder() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docBuilder = docFactory.newDocumentBuilder();
            doc = docBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Unable to initialize doc builder.");
        }
    }

    public String generateIvy() throws Exception {
        log.debug("mavenproject is null? " + (mavenProject == null));
        log.debug("artifact is null? " + (artifact == null));

        if (mavenProject == null && artifact != null)
            return generateIvyXmlForArtifact();

        Element rootElement = ivyModule();
        rootElement.appendChild(info(mavenProject.getGroupId(), mavenProject.getArtifactId(), mavenProject.getVersion()));
        rootElement.appendChild(dependencies());
        doc.appendChild(rootElement);

        return createXmlString();
    }

    public String generateIvyXmlForArtifact() throws Exception {
        Element ivyModule = ivyModule();
        ivyModule.appendChild(info(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
        ivyModule.appendChild(publications());
        ivyModule.appendChild(dependencies());
        doc.appendChild(ivyModule);

        return createXmlString();
    }

    private String createXmlString() throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(doc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);

        return writer.toString();
    }

    private Element ivyModule() {
        Element ivyModule = doc.createElement("ivy-module");
        ivyModule.setAttribute("version", "1.0");
        return ivyModule;
    }

    private Element info(String org, String module, String rev) {
        Element info = doc.createElement("info");
        info.setAttribute("organisation", org);
        info.setAttribute("module", module);
        info.setAttribute("revision", rev);

        return info;
    }

    private Element publications() {
        Element publications = doc.createElement("publications");
        publications.appendChild(artifact());

        return publications;
    }

    private Element artifact() {
        Element artifact = doc.createElement("artifact");
        artifact.setAttribute("name", this.artifact.getArtifactId() + "-" + this.artifact.getVersion());
        artifact.setAttribute("type", this.artifact.getType());
        return artifact;
    }

    private Element dependencies() {
        Element dependencies = doc.createElement("dependencies");

        if (mavenProject != null && mavenProject.getDependencies().size() != 0) {
            for (Artifact artifact : artifacts) {
                dependencies.appendChild(dependency(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion()));
            }
        }

        return dependencies;
    }

    private Element dependency(String org, String name, String rev) {
        if (rev.startsWith("${")) {
            String propName = rev.substring(rev.indexOf("{") + 1, rev.indexOf("}"));
            rev = mavenProject.getProperties().getProperty(propName);
        }
        Element dependency = doc.createElement("dependency");
        dependency.setAttribute("org", org);
        dependency.setAttribute("name", name);
        dependency.setAttribute("rev", rev);

        return dependency;
    }
}
