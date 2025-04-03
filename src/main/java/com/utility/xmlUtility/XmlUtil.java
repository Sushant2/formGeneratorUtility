package com.utility.xmlUtility;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

public class XmlUtil {
    public static String elementToString(Element element) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));

            return writer.toString();
        } catch (Exception e) {
            return "⚠️ Error converting element to string: " + e.getMessage();
        }
    }

    // Extract header-name from a field
    public static String getHeaderNameBySection(Document doc, String sectionValue) {
        NodeList headers = doc.getElementsByTagName("header");
    
        for (int i = 0; i < headers.getLength(); i++) {
            Element header = (Element) headers.item(i);
            NodeList sectionNodes = header.getElementsByTagName("section");
    
            if (sectionNodes.getLength() > 0) {
                String sectionText = sectionNodes.item(0).getTextContent().trim();
    
                if (sectionValue.equals(sectionText)) {
                    return header.getAttribute("name").trim(); // Return the header name from target XML
                }
            }
        }
        return ""; // Return empty if no matching header is found
    }
    

    // Extract section number from a field
    public static String getSection(Element fieldElement) {
        NodeList sectionNodes = fieldElement.getElementsByTagName("section");
        if (sectionNodes.getLength() > 0) {
            return sectionNodes.item(0).getTextContent().trim();
        }
        return "";
    }

    public static Element findHeaderByName(Document doc, String headerName) {
        NodeList headers = doc.getElementsByTagName("header");
    
        for (int i = 0; i < headers.getLength(); i++) {
            Element header = (Element) headers.item(i);
            if (header.hasAttribute("name") && header.getAttribute("name").equals(headerName)) {
                return header; // Found the correct header, return it
            }
        }
        return null; // No matching header found
    }
    

    // Get the last order-by value in a section
    public static int getLastOrderBy(Element section) {
        if (section == null) {
            System.out.println("⚠️ Warning: getLastOrderBy() received a null section.");
            return 0; // Default order-by value
        }
    
        NodeList fields = section.getElementsByTagName("field");
        int maxOrderBy = 0;
    
        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            if (field.hasAttribute("order-by")) {
                maxOrderBy = Math.max(maxOrderBy, Integer.parseInt(field.getAttribute("order-by")));
            }
        }
        return maxOrderBy;
    }    

    public static String extractDbFieldValue(Element element) {
        NodeList dbFieldNodes = element.getElementsByTagName("db-field");
        if (dbFieldNodes.getLength() > 0) {
            return dbFieldNodes.item(0).getTextContent().trim();
        }
        return "";
    }

    public static Element findOrCreateParent(Document doc, String parentTagName) {
        NodeList nodeList = doc.getElementsByTagName(parentTagName);
        if (nodeList.getLength() > 0) {
            return (Element) nodeList.item(0);
        } else {
            // Create the parent tag if it doesn't exist
            Element newParent = doc.createElement(parentTagName);
            doc.getDocumentElement().appendChild(newParent);
            return newParent;
        }
    }

    public static Set<String> extractElements(Document doc, String tagName) {
        Set<String> elements = new HashSet<>();
        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add(getElementAttributeOrText((Element) node, "name"));
            }
        }
        return elements;
    }

    public static String getElementAttributeOrText(Element element, String attribute) {
        if (element.hasAttribute(attribute)) {
            return element.getAttribute(attribute).trim();
        } else {
            return element.getTextContent().trim();
        }
    }

    public static Document createNewXmlDocument(String rootElementName) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element rootElement = doc.createElement(rootElementName);
        doc.appendChild(rootElement);
        return doc;
    }

    public static Document loadXmlDocument(String filePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new File(filePath));
    }

    public static void saveXmlDocument(Document doc, String filePath) throws TransformerException {
        doc.normalizeDocument();
        removeEmptyTextNodes(doc);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty("{http://xml.apache.org/xalan}line-separator", "\n");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(filePath));
        transformer.transform(source, result);
    }

    public static void removeEmptyTextNodes(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = children.getLength() - 1; i >= 0; i--) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty()) {
                node.removeChild(child);
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeEmptyTextNodes(child);
            }
        }
    }
}
