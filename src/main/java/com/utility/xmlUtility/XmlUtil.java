package com.utility.xmlUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    public static Set<String> extractElements(Document doc, String tagName, String attributeName) {
        Set<String> elements = new HashSet<>();
        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                elements.add(getElementAttributeOrText((Element) node, attributeName));
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

        // Traverse all headers to also group table-fields by section
        NodeList headers = targetDoc.getElementsByTagName("header");
        for (int i = 0; i < headers.getLength(); i++) {
            Element header = (Element) headers.item(i);
            String section = XmlUtil.getSection(header);

            if (section == null || section.isEmpty())
                continue;

            NodeList dependentTables = header.getElementsByTagName("dependent-table");

            for (int j = 0; j < dependentTables.getLength(); j++) {
                Element table = (Element) dependentTables.item(j);
                NodeList tableFields = table.getElementsByTagName("table-field");

                for (int k = 0; k < tableFields.getLength(); k++) {
                    Element tableField = (Element) tableFields.item(k);
                    sectionFieldsMap.computeIfAbsent(section, k1 -> new ArrayList<>()).add(tableField);
                }
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
                boolean isActive = true;
            
                // For <field>, check <is-active> tag
                NodeList isActiveNodes = field.getElementsByTagName("is-active");
                if (isActiveNodes.getLength() > 0) {
                    String isActiveValue = isActiveNodes.item(0).getTextContent().trim().toLowerCase();
                    isActive = !isActiveValue.equals("no");
                }
            
                // For <table-field>, check attribute "isActive"
                if (field.getTagName().equals("table-field")) {
                    String attr = field.getAttribute("isActive");
                    if (attr != null && attr.trim().equalsIgnoreCase("no")) {
                        isActive = false;
                    }
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
        System.out.println("Setting <order-by> to " + value + " for field: " + XmlUtil.nodeToString(field));

        // Set as attribute for table-field only
        String tagName = field.getTagName();
        if ("table-field".equals(tagName)) {
            field.setAttribute("order-by", String.valueOf(value)); // handles <table-field>
        }
        else{
            // fallback for <field><order-by>...</order-by></field>
            NodeList orderByNodes = field.getElementsByTagName("order-by");
            if (orderByNodes.getLength() > 0) {
                orderByNodes.item(0).setTextContent(String.valueOf(value));
            }
            // remove attribute if it exists
            field.removeAttribute("order-by");
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

    public static void updateTagsIfDiff(Element sourceField, Map<String, Element> targetFieldMap) {
        String sourceDbField = XmlUtil.getValue(sourceField, "db-field").trim().toUpperCase();
        String sourceDisplayName = XmlUtil.getValue(sourceField, "display-name").trim();
        String sourceIsMandatory = XmlUtil.getValue(sourceField, "is-mandatory").trim().toLowerCase();
        String sourceIsActive = XmlUtil.getValue(sourceField, "is-active").trim().toLowerCase();

        Element targetField = targetFieldMap.get(sourceDbField);

        if (targetField != null) {
            // Update display-name if different
            String targetDisplayName = XmlUtil.getValue(targetField, "display-name").trim();
            if (!targetDisplayName.equals(sourceDisplayName)) {
                XmlUtil.replaceOrInsertChild(targetField, "display-name", sourceDisplayName);
                System.out.println("Updated display-name of db-field '" + sourceDbField + "' from '" + targetDisplayName
                        + "' to → '" + sourceDisplayName + "'");
            }

            // Update is-mandatory if different
            String targetIsMandatory = XmlUtil.getValue(targetField, "is-mandatory").trim().toLowerCase();
            if (!targetIsMandatory.equals(sourceIsMandatory)) {
                XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", sourceIsMandatory);
                System.out.println("Updated is-mandatory of db-field '" + sourceDbField + "' from '" + targetIsMandatory
                        + "' to → '" + sourceIsMandatory + "'");
            }

            // Update is-active if different
            String targetIsActive = XmlUtil.getValue(targetField, "is-active").trim().toLowerCase();
            if (!targetIsActive.equals(sourceIsActive)) {
                XmlUtil.replaceOrInsertChild(targetField, "is-active", sourceIsActive);
                System.out.println("Updated is-active of db-field '" + sourceDbField + "' from '" + targetIsActive
                        + "' to → '" + sourceIsActive + "'");
            }
        }
    }    

    public static Map<String, Element> buildTargetFieldMap(Document targetDoc) {
        Map<String, Element> map = new HashMap<>();
        NodeList allTargetFields = targetDoc.getElementsByTagName("field");

        for (int i = 0; i < allTargetFields.getLength(); i++) {
            Element field = (Element) allTargetFields.item(i);
            String dbField = XmlUtil.getValue(field, "db-field").trim().toUpperCase();
            if (!dbField.isEmpty()) {
                map.put(dbField, field);
            }
        }
        return map;
    }

    public static Map<String, String> readTableMappings(String xmlFilePath) {
        Map<String, String> tableMappings = new HashMap<>();

        try {
            Document tableMappingDoc = XmlUtil.loadXmlDocument(xmlFilePath);

            NodeList mappings = tableMappingDoc.getElementsByTagName("table-mapping");

            for (int i = 0; i < mappings.getLength(); i++) {
                Element mapping = (Element) mappings.item(i);

                String tableAnchor = mapping.getAttribute("table-anchor");
                String fileLocation = mapping.getAttribute("filelocation");

                // Only put if both are non-empty
                if (!tableAnchor.isEmpty() && !fileLocation.isEmpty()) {
                    tableMappings.put(tableAnchor, fileLocation);
                }
            }
        } catch (Exception e) {
            System.err.println("Error reading table mappings: " + e.getMessage());
        }

        return tableMappings;
    }

    public static Map<String, Element> readTabModules(String xmlFilePath) {
        Map<String, Element> tabModuleElements = new HashMap<>();

        try {
            Document tabModuleDoc = XmlUtil.loadXmlDocument(xmlFilePath);

            NodeList moduleTabs = tabModuleDoc.getElementsByTagName("module-tab");

            for (int i = 0; i < moduleTabs.getLength(); i++) {
                Element element = (Element) moduleTabs.item(i);
                String dbTable = element.getAttribute("db-table");
                if (dbTable == null || dbTable.isEmpty()) continue;
            
                tabModuleElements.put(dbTable, element);
            }
            
        } catch (Exception e) {
            System.err.println("Error reading tab modules: " + e.getMessage());
        }

        return tabModuleElements;
    }

    public static void processCustomModulesXml(Map<String, Element> sourceTabModuleElements, Map<String, Element> targetTabModuleElements, String sourceTabModulesPath, String targetTabModulesPath) {
        try {

            Document targetDoc = XmlUtil.loadXmlDocument(targetTabModulesPath);
            Element targetRoot = targetDoc.getDocumentElement();
            boolean updated = false;

            for (Map.Entry<String, Element> entry : sourceTabModuleElements.entrySet()) {
                String dbTable = entry.getKey();
                Element sourceTabElement = entry.getValue();

                if (!targetTabModuleElements.containsKey(dbTable)) {
                    Node importedNode = targetDoc.importNode(sourceTabElement, true);
                    targetRoot.appendChild(importedNode);
                    updated = true;
                    System.out.println("Added missing <module-tab> with db-table: " + dbTable);
                }
            }

            if (updated) {
                XmlUtil.saveXmlDocument(targetDoc, targetTabModulesPath); // Overwrite or write to new file
                System.out.println("Target tabmodules.xml updated with new entries.");
            } else {
                System.out.println("No updates required. All module-tabs are already present.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to sync module-tabs: " + e.getMessage());
        }
    }

    public static void copyTableContent(Document sourceDoc, Document targetDoc) {
        // Get the <table> element from the source
        Element sourceTable = (Element) sourceDoc.getElementsByTagName("table").item(0);

        if (sourceTable != null) {
            NodeList childNodes = sourceTable.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String nodeName = node.getNodeName();

                    // Stop copying if you encounter <field>
                    if ("field".equals(nodeName)) {
                        System.out.println("Encountered <field>, stopping the copy.");
                        break;
                    }

                    // Otherwise, copy the node
                    Node importedNode = targetDoc.importNode(node, true);
                    targetDoc.getDocumentElement().appendChild(importedNode);
                }
            }
        }
    }

    public static String generateInsertQuery(String targetKeyPath, String filePath, String module) throws Exception {
        // Generate query
        try {
            String xmlFilename = new File(targetKeyPath).getName(); // e.g. "franchiseesky.xml"
            String data = new String(Files.readAllBytes(Paths.get(targetKeyPath)), StandardCharsets.UTF_8)
                    .replace("'", "''"); // escape single quotes for SQL
            String moduleName = module;

            // If module is not provided, extract it from the file path
            if (moduleName == null || moduleName.isEmpty()) {
                int tablesIndex = filePath.indexOf("tables/");
                if (tablesIndex != -1) {
                    String afterTables = filePath.substring(tablesIndex + 7); // skip "tables/"
                    int slashIndex = afterTables.indexOf("/");
                    if (slashIndex != -1) {
                        moduleName = afterTables.substring(0, slashIndex);
                    }
                }
            }

            // Extract XML_KEY from the filename (remove .xml)
            String xmlKey = xmlFilename.replace(".xml", "");

            StringBuilder query = new StringBuilder();
            // delete query for xmlkey
            query.append("DELETE FROM CLIENT_XMLS WHERE XML_KEY = '").append(xmlKey).append("';");
            query.append(System.lineSeparator());

            // delete query for xmlkey_copy
            query.append("DELETE FROM CLIENT_XMLS WHERE XML_KEY = '").append(xmlKey).append("_copy").append("';");

            query.append(System.lineSeparator());

            // insert query for xmlkey
            query.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
            query.append("NULL, ");
            query.append("'").append(xmlFilename).append("', ");
            query.append("'").append(xmlKey).append("', ");
            query.append("'").append(moduleName).append("', ");
            query.append("'").append(filePath).append("', ");
            query.append("'").append(data).append("', ");
            query.append("CURRENT_TIMESTAMP);");

            query.append(System.lineSeparator());

            // insert query for xmlkey_copy
            String copiedXmlFilename = xmlFilename.replace(".xml", "_copy.xml");
            String copiedFilePath = filePath.replace(".xml", "_copy.xml");

            query.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
            query.append("NULL, ");
            query.append("'").append(copiedXmlFilename).append("', ");
            query.append("'").append(xmlKey).append("_copy").append("', ");
            query.append("'").append(moduleName).append("', ");
            query.append("'").append(copiedFilePath).append("', ");
            query.append("'").append(data).append("', ");
            query.append("CURRENT_TIMESTAMP);");

            String insertQuery = query.toString();

            System.out.println("Generated Query: " + insertQuery);

            return insertQuery;
        } catch (Exception e) {
            System.out.println("Error generating insert query: " + e.getMessage());
            return null;
        }
    }
    
    public static void writeToFile(String filePath, List<String> queryList) throws Exception {
        File outputFile = new File(filePath);
        outputFile.getParentFile().mkdirs();
        // Create the file if it doesn't exist
        if (!outputFile.exists()) {
            outputFile.createNewFile();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
            for (String currQuery : queryList) {
                if (currQuery == null || currQuery.isEmpty()) {
                    System.out.println("Skipping empty query");
                    continue;
                }
                writer.write(currQuery);
                writer.newLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static void appendToCustomFieldsCSV(String customFieldName) {
        String filePath = "src/main/resources/customFields.csv";

        try (FileWriter writer = new FileWriter(filePath, true)) { // true = append mode
            writer.append(customFieldName);
            writer.append('\n'); // add new line
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}