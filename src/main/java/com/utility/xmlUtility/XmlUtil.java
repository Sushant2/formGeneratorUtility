package com.utility.xmlUtility;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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
            return "Error converting element to string: " + e.getMessage();
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
    /*public static int getLastOrderBy(Document doc, String sectionValue) {
        NodeList fields = doc.getElementsByTagName("field");
        int maxOrderBy = 0;
    
        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
    
            // Match section inside the <field>
            NodeList sectionNodes = field.getElementsByTagName("section");
            if (sectionNodes.getLength() > 0) {
                String sectionText = sectionNodes.item(0).getTextContent().trim();
    
                if (sectionValue.equals(sectionText)) {
                    // Look for order-by inside the field
                    NodeList orderByNodes = field.getElementsByTagName("order-by");
                    if (orderByNodes.getLength() > 0) {
                        try {
                            int orderByValue = Integer.parseInt(orderByNodes.item(0).getTextContent().trim());
                            maxOrderBy = Math.max(maxOrderBy, orderByValue);
                        } catch (NumberFormatException e) {
                            System.out.println("Skipping invalid order-by value.");
                        }
                    }
                }
            }
        }
    
        return maxOrderBy;
    }*/

    public static int getLastOrderBy(Document doc, String sectionValue) {
        NodeList fields = doc.getElementsByTagName("field");
        int maxOrderBy = 0;
    
        System.out.println("Looking for highest order-by in section: " + sectionValue);
        System.out.println("Total <field> elements found: " + fields.getLength());
    
        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            System.out.println(elementToString(field));
            
            NodeList sectionNodes = field.getElementsByTagName("section");
            if (sectionNodes.getLength() > 0) {
                String sectionText = sectionNodes.item(0).getTextContent().trim();
                System.out.println("Found <section>: " + sectionText);
    
                if (sectionValue.equals(sectionText)) {
                    NodeList orderByNodes = field.getElementsByTagName("order-by");
                    if (orderByNodes.getLength() > 0) {
                        String orderText = orderByNodes.item(0).getTextContent().trim();
                        System.out.println("Found <order-by>: " + orderText);
    
                        try {
                            int orderByValue = Integer.parseInt(orderText);
                            if (orderByValue > maxOrderBy) {
                                System.out.println("Updating maxOrderBy: " + maxOrderBy + " → " + orderByValue);
                                maxOrderBy = orderByValue;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Skipping invalid order-by value: " + orderText);
                        }
                    } else {
                        System.out.println("No <order-by> inside matching <field> with section: " + sectionText);
                    }
                }
            } else {
                System.out.println("<field> element has no <section>.");
            }
        }
    
        System.out.println("Final maxOrderBy for section " + sectionValue + ": " + maxOrderBy);
        return maxOrderBy;
    }
    
    

    public static String nodeToString(Node node) {
        if (node == null) {
            return "Warning: nodeToString() received a null node.";
        }

        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            return "Error converting node to string: " + e.getMessage();
        }
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

    public static void fixOrderByPerSection(Document targetDoc) {
        Map<String, List<Element>> sectionFieldsMap = new HashMap<>();
        NodeList allFields = targetDoc.getElementsByTagName("field");
    
        // Group all <field> elements by their <section> value
        for (int i = 0; i < allFields.getLength(); i++) {
            Element field = (Element) allFields.item(i);
            String section = XmlUtil.getSection(field);
    
            if (section != null && !section.isEmpty()) {
                sectionFieldsMap.computeIfAbsent(section, k -> new ArrayList<>()).add(field);
            }
        }
    
        // Now reassign <order-by> starting from 0 within each section
        for (Map.Entry<String, List<Element>> entry : sectionFieldsMap.entrySet()) {
            String section = entry.getKey();
            List<Element> fields = entry.getValue();
    
            System.out.println("Reassigning <order-by> for section " + section);

            List<Element> activeFields = new ArrayList<>();
            List<Element> inactiveFields = new ArrayList<>();
    
            for (Element field : fields) {
                NodeList isActiveNodes = field.getElementsByTagName("is-active");
                boolean isActive = true;
    
                if (isActiveNodes.getLength() > 0) {
                    String isActiveValue = isActiveNodes.item(0).getTextContent().trim().toLowerCase();
                    isActive = !isActiveValue.equals("no");
                }
    
                if (isActive) {
                    activeFields.add(field);
                } else {
                    inactiveFields.add(field);
                }
            }

            int order = 0;

            // First assign to active fields
            for (Element field : activeFields) {
                XmlUtil.setOrderBy(field, order++);
            }
    
            // Then assign to inactive fields
            for (Element field : inactiveFields) {
                XmlUtil.setOrderBy(field, order++);
            }
        }
    
        System.out.println("All <order-by> values normalized per section.");
    }

    public static void setOrderBy(Element field, int value) {
        NodeList orderByNodes = field.getElementsByTagName("order-by");
        if (orderByNodes.getLength() > 0) {
            orderByNodes.item(0).setTextContent(String.valueOf(value));
        }
    }
    public static Element stringToElement(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xmlString)));

        // Return the first element (inside the document root)
        return doc.getDocumentElement();
    }

    public static String getDisplayType(Element fieldElement) {
        NodeList nodes = fieldElement.getElementsByTagName("display-type");
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }

    public static Boolean isMultiSelect(Element fieldElement) {
        NodeList nodes = fieldElement.getElementsByTagName("is-multiselect-fimSearch");
        if (nodes.getLength() > 0) {
            return true;
        }
        return false;
    }

    public static String getValue(Element element, String tagName) {
        NodeList nodes = element.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return "";
    }
    
    public static void replaceChildValue(Element parent, String tagName, String newValue) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            nodes.item(0).setTextContent(newValue);
        }
    }
    
    public static void replaceOrInsertChild(Element parent, String tagName, String value) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            nodes.item(0).setTextContent(value);
        } else {
            Element newElement = parent.getOwnerDocument().createElement(tagName);
            newElement.setTextContent(value);
            parent.appendChild(newElement);
        }
    }

    public static Element getDirectChildNode(Element parent, String tagName) {
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && tagName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null; // No direct child with the given tagName found
    }
    
    public static String generateInsertQuery(String targetKeyPath, String filePath){

        String xmlFilename = new File(targetKeyPath).getName();           // e.g. "franchiseesky.xml"
        String module = "";
        String data = "";
        try{
            data = new String(Files.readAllBytes(Paths.get(targetKeyPath)), StandardCharsets.UTF_8).replace("'", "''"); // escape single quotes for SQL
        }catch(Exception e){
            System.out.println("Error reading file: " + e.getMessage());
            return null;
        }

        int tablesIndex = filePath.indexOf("tables/");
        if (tablesIndex != -1) {
            String afterTables = filePath.substring(tablesIndex + 7); // skip "tables/"
            int slashIndex = afterTables.indexOf("/");
            if (slashIndex != -1) {
                module = afterTables.substring(0, slashIndex);
            }
        }

        // Extract XML_KEY from the filename (remove .xml)
        String xmlKey = xmlFilename.replace(".xml", "");


        StringBuilder insertQuery = new StringBuilder();
        insertQuery.append("DELETE FROM CLIENT_XMLS WHERE XML_KEY = '").append(xmlKey).append("';");
        insertQuery.append(System.lineSeparator());
        insertQuery.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
        insertQuery.append("NULL, ");
        insertQuery.append("'").append(xmlFilename).append("', ");
        insertQuery.append("'").append(xmlKey).append("', ");
        insertQuery.append("'").append(module).append("', ");
        insertQuery.append("'").append(filePath).append("', ");
        insertQuery.append("'").append(data).append("', ");
        insertQuery.append("CURRENT_TIMESTAMP);");
        
        System.out.println("Generated Query: " + insertQuery);

        return insertQuery.toString();
    }
    
}