package com.insightwrite.security;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyVersionSecurityTest {

    @Test
    void springBootParentUsesSecurityPatchedBoot35Line() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder()
                .parse(Path.of("pom.xml").toFile());

        Element parent = (Element) document.getElementsByTagNameNS("*", "parent").item(0);
        String version = text(parent, "version");

        assertThat(version).isEqualTo("3.5.14");
    }

    private String text(Element parent, String name) {
        NodeList nodes = parent.getElementsByTagNameNS("*", name);
        return nodes.item(0).getTextContent().trim();
    }
}
