package com.utility.xmlUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.*;

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

    // Tracks which fields already produced UPDATE statements to avoid duplicates.
    private static final Set<String> processedUnderscoreFields = new HashSet<>();
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

    // Find the parent header element of a field by traversing up the DOM tree
    public static Element findParentHeader(Element fieldElement) {
        Node parent = fieldElement.getParentNode();
        while (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
            Element parentElement = (Element) parent;
            if ("header".equals(parentElement.getTagName())) {
                return parentElement;
            }
            parent = parent.getParentNode();
        }
        return null;
    }

    public static Element findHeaderByName(Document doc, String headerName) {
        NodeList headers = doc.getElementsByTagName("header");
    
        for (int i = 0; i < headers.getLength(); i++) {
            Element header = (Element) headers.item(i);
            if (header.hasAttribute("name") && header.getAttribute("name").startsWith(headerName)) {
                return header; // Found the correct header, return it
            }
        }
        return null; // No matching header found
    }

    public static Element findHeaderBybSecName(Document doc, String headerName) {
        NodeList headers = doc.getElementsByTagName("header");
        for (int i = 0; i < headers.getLength(); i++) {
            Element header = (Element) headers.item(i);
            if (header.hasAttribute("name") && header.getAttribute("name").startsWith("bSec" + headerName)) {
                return header; // Found the correct header, return it
            }
        }
        return null; // No matching header found
    }

    public static Element findHeaderByValue(Document doc, String headerValue) {
        NodeList headers = doc.getElementsByTagName("header");
        for (int i = 0; i < headers.getLength(); i++) {
            Element header = (Element) headers.item(i);
            if (header.hasAttribute("value") && headerValue.equals(header.getAttribute("value"))) {
                return header; // Found the correct header by value, return it
            }
        }
        return null; // No matching header found
    }

    public static Element findForeignTableByName(Document doc, String foreignTableName) {
        NodeList foreignTables = doc.getElementsByTagName("foreign-table");
        for (int i = 0; i < foreignTables.getLength(); i++) {
            Element foreignTable = (Element) foreignTables.item(i);
            if (foreignTable.hasAttribute("name") && foreignTable.getAttribute("name").startsWith(foreignTableName)) {
                return foreignTable; // Found the correct foreign table, return it
            }
        }
        return null; // No matching foreign table found
    }

    public static Element findFieldByDbField(Document doc, String dbFieldName) {
        NodeList fields = doc.getElementsByTagName("field");
        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            String fieldDbField = XmlUtil.getValue(field, "db-field").trim().toUpperCase();
            if (fieldDbField.equals(dbFieldName.toUpperCase())) {
                return field; // Found the field with matching db-field, return it
            }
        }
        return null; // No matching field found
    }

    public static Element findFieldByFieldName(Document doc, String fieldName) {
        NodeList fields = doc.getElementsByTagName("field");
        for (int i = 0; i < fields.getLength(); i++) {
            Element field = (Element) fields.item(i);
            String fieldNameValue = XmlUtil.getValue(field, "field-name").trim();
            if (fieldNameValue.equals(fieldName)) {
                return field; // Found the field with matching field-name, return it
            }
        }
        return null; // Field not found
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
            } else {
                // Create new order-by element if it doesn't exist
                Document doc = field.getOwnerDocument();
                Element orderByElement = doc.createElement("order-by");
                orderByElement.setTextContent(String.valueOf(value));
                field.appendChild(orderByElement);
                System.out.println("Created new <order-by> element with value: " + value);
            }
            // remove attribute if it exists
            field.removeAttribute("order-by");
        }
    }

    public static void setHeaderOrders(Document targetDoc) {
        NodeList headers = targetDoc.getElementsByTagName("header");
        for (int i = 0; i < headers.getLength(); i++) {
            Element header = (Element) headers.item(i);
            // Set the order attribute directly on the header element
            header.setAttribute("order", String.valueOf(i));
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
        NodeList nodes = fieldElement.getElementsByTagName("is-multiselect");
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
        // Ensure is-active and is-mandatory always have values, default to "yes" if empty
        if ("is-active".equals(tagName) && (newValue == null || newValue.trim().isEmpty())) {
            newValue = "yes";
        } else if ("is-mandatory".equals(tagName) && (newValue == null || newValue.trim().isEmpty())) {
            newValue = "false";
        }
        
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            nodes.item(0).setTextContent(newValue);
        }
    }
    
    public static void replaceOrInsertChild(Element parent, String tagName, String value) {
        // Ensure is-active and is-mandatory always have values, default to "yes" if empty
        if ("is-active".equals(tagName) && (value == null || value.trim().isEmpty())) {
            value = "yes";
        } else if ("is-mandatory".equals(tagName) && (value == null || value.trim().isEmpty())) {
            value = "false";
        }
        
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

    public static void updateTagsIfDiff(Element sourceField, Map<String, Element> targetFieldMap, Map<String, String> updatedHeaders, Document sourceDoc, Document targetDoc, String sourcePath) {
        String sourceDbField = XmlUtil.getValue(sourceField, "db-field").trim().toUpperCase();
        String sourceDisplayName = XmlUtil.getValue(sourceField, "display-name").trim();
        String sourceIsMandatory = XmlUtil.getValue(sourceField, "is-mandatory").trim().toLowerCase();
        String sourceIsActive = XmlUtil.getValue(sourceField, "is-active").trim().toLowerCase();

        Element targetField = targetFieldMap.get(sourceDbField);

        if (targetField != null) {

            // Check if source has order-by but target doesn't
            String sourceOrderBy = XmlUtil.getValue(sourceField, "order-by").trim();
            String targetOrderBy = XmlUtil.getValue(targetField, "order-by").trim();
            
            if (!sourceOrderBy.isEmpty() && targetOrderBy.isEmpty()) {
                // Add the required tags from source to target
                String sourceSection = XmlUtil.getValue(sourceField, "section").trim();
                String sourceDisplayType = XmlUtil.getValue(sourceField, "display-type").trim();
                String sourceBuildField = XmlUtil.getValue(sourceField, "build-field").trim();
                
                // Add section tag
                if (!sourceSection.isEmpty()) {
                    // Get section from sourceField, find header name in source doc, check updatedHeaders map,
                    // then find header in target doc and get its section to update targetField
                    
                    // Step 1: Find header name in source document that has the sourceSection
                    String sourceHeaderName = XmlUtil.getHeaderNameBySection(sourceDoc, sourceSection);
                    if (!sourceHeaderName.isEmpty()) {
                        // Step 2: Check if header name exists in updatedHeaders map, use mapped value if available
                        String targetHeaderName = sourceHeaderName;
                        if (updatedHeaders != null && updatedHeaders.containsKey(sourceHeaderName)) {
                            targetHeaderName = updatedHeaders.get(sourceHeaderName);
                        }
                        
                        // Step 3: Find header in target document by name
                        Element targetHeader = XmlUtil.findHeaderByName(targetDoc, targetHeaderName);
                        if (targetHeader != null) {
                            // Step 4: Get section from target header (e.g., "bSec_additionalInformation1350065")
                            String targetSection = XmlUtil.getSection(targetHeader);
                            if (!targetSection.isEmpty()) {
                                // Step 5: Update targetField with target section
                                XmlUtil.replaceOrInsertChild(targetField, "section", targetSection);
                                System.out.println("Updated section '" + targetSection + "' in target field '" + sourceDbField + "' (header: '" + targetHeaderName + "')");
                            }
                        }
                    }
                }
                
                // Add order-by tag
                XmlUtil.replaceOrInsertChild(targetField, "order-by", sourceOrderBy);
                System.out.println("Added order-by '" + sourceOrderBy + "' to db-field '" + sourceDbField + "'");
                
                // Add display-type tag
                if (!sourceDisplayType.isEmpty()) {
                    // Handle display-type conversion same as in XmlService
                    if(sourceDisplayType.equals("Label")){
                        String dataType = XmlUtil.getValue(sourceField, "data-type");
                        if(dataType.equals("Date")){
                            sourceDisplayType = "Date";
                        }else if(dataType.equals("String")){
                            sourceDisplayType = "Text";
                        }
                    }
                    XmlUtil.replaceOrInsertChild(targetField, "display-type", sourceDisplayType);
                    System.out.println("Added display-type '" + sourceDisplayType + "' to db-field '" + sourceDbField + "'");
                }
                
                // Add active tag (using is-active value)
                XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                System.out.println("Added is-active yes to db-field '" + sourceDbField + "'");

                if(sourcePath != null && (sourcePath.contains("fimTransfer.xml") || sourcePath.contains("fimTransfer_copy.xml"))){
                // Special handling for Buyer Details - set is-active to "no" for fimTransfer.xml
                    if("BUYER_EXISTING_OR_NEW_FRANCHISEE".equals(sourceDbField) || "FIRST_NAME".equals(sourceDbField) || "LAST_NAME".equals(sourceDbField) || "FRANCHISE_OWNER_ID".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "no");
                        System.out.println("Updated is-active element to 'no' for field: " + sourceDbField);
                    }
                    if("TRANSFER_FEE_PD".equals(sourceDbField) || "TRANSFER_FEE_NUMERICAL".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-active element to 'yes' for field: " + sourceDbField);
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimRenewal.xml") || sourcePath.contains("fimRenewal_copy.xml"))){
                    if("FIM_CB_CURRENT_STATUS".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-active element to 'yes' for field: " + sourceDbField);
                    }
                }

                if(sourcePath != null && (sourcePath.contains("franchisees.xml") || sourcePath.contains("franchisees_copy.xml"))){
                    // Special handling for STORE_OPENING_DATE as inactive
                    if("STORE_OPENING_DATE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "no");
                        System.out.println("Set is-active element with value 'no' to STORE_OPENING_DATE field");
                    }
                }
                
                // Add build-field tag
                if (!sourceBuildField.isEmpty()) {
                    XmlUtil.replaceOrInsertChild(targetField, "build-field", sourceBuildField);
                    System.out.println("Added build-field '" + sourceBuildField + "' to db-field '" + sourceDbField + "'");
                }

                // Add display-name tag
                if (!sourceDisplayName.isEmpty()) {
                    XmlUtil.replaceOrInsertChild(targetField, "display-name", sourceDisplayName);
                    System.out.println("Added display-name '" + sourceDisplayName + "' to db-field '" + sourceDbField + "'");
                }
                
                // Add is-mandatory tag
                XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", sourceIsMandatory);
                System.out.println("Added is-mandatory '" + sourceIsMandatory + "' to db-field '" + sourceDbField + "'");

                // Add pii-enabled tag
                XmlUtil.replaceOrInsertChild(targetField, "pii-enabled", "false");
                System.out.println("Added section pii-enabled to db-field false'");

                // Add group-by tag
                XmlUtil.replaceOrInsertChild(targetField, "group-by", "true");
                System.out.println("Added section group-by to db-field true'");

            } else {
                // Regular handling for other fields
                // Update display-name if different
                String targetDisplayName = XmlUtil.getValue(targetField, "display-name").trim();
                if ("GRAND_STORE_OPENING_DATE".equals(sourceDbField)) {
                    XmlUtil.replaceOrInsertChild(targetField, "display-name", "Expected Store Opening Date");
                    System.out.println("Updated display-name of db-field '" + sourceDbField + "' from '" + targetDisplayName + "' to → 'Expected Opening Date' (special handling for GRAND_STORE_OPENING_DATE)");
                }else if ("AREA_ID".equals(sourceDbField)) {
                    XmlUtil.replaceOrInsertChild(targetField, "display-name", "Area Franchise ID");
                    System.out.println("Updated display-name of db-field '" + sourceDbField + "' from '" + targetDisplayName + "' to → 'Area Franchise ID' (special handling for AREA_ID)");
                }else if ("FBC".equals(sourceDbField)) {
                    XmlUtil.replaceOrInsertChild(targetField, "display-name", "Supervisor");
                    System.out.println("Updated display-name of db-field '" + sourceDbField + "' from '" + targetDisplayName + "' to → 'Supervisor' (special handling for FBC)");
                }else if ("STATUS".equals(sourceDbField)) {
                    XmlUtil.replaceOrInsertChild(targetField, "display-name", "Type");
                    System.out.println("Updated display-name of db-field '" + sourceDbField + "' from '" + targetDisplayName + "' to → 'Type' (special handling for STATUS)");
                }else if (!targetDisplayName.equals(sourceDisplayName)) {
                    XmlUtil.replaceOrInsertChild(targetField, "display-name", sourceDisplayName);
                    System.out.println("Updated display-name of db-field '" + sourceDbField + "' from '" + targetDisplayName + "' to → '" + sourceDisplayName + "'");
                }

                // Update is-mandatory if different
                String targetIsMandatory = XmlUtil.getValue(targetField, "is-mandatory").trim().toLowerCase();
                if (!targetIsMandatory.equals(sourceIsMandatory)) {
                    XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", sourceIsMandatory);
                    System.out.println("Updated is-mandatory of db-field '" + sourceDbField + "' from '" + targetIsMandatory + "' to → '" + sourceIsMandatory + "'");
                }

                // Update is-active if different
                String targetIsActive = XmlUtil.getValue(targetField, "is-active").trim().toLowerCase();
                if (!targetIsActive.equals(sourceIsActive)) {
                    if("AREA_ID".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-active of db-field '" + sourceDbField + "' from '" + targetIsActive + "' to → 'yes' (special handling for AREA_ID)");
                    }
                    else {
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", sourceIsActive);
                        System.out.println("Updated is-active of db-field '" + sourceDbField + "' from '" + targetIsActive + "' to → '" + sourceIsActive + "'");
                    }
                }
            
                if(sourcePath != null && (sourcePath.contains("franchisees.xml") || sourcePath.contains("franchisees_copy.xml"))){
                    // Special handling for OPENING_DATE as active
                    if("OPENING_DATE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Set is-active element with value 'yes' to OPENING_DATE field");
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimTransfer.xml") || sourcePath.contains("fimTransfer_copy.xml"))){
                    // Special handling for TRANSFER_FEE_PD and TRANSFER_FEE_NUMERICAL - always set is-active to "yes"
                    if("TRANSFER_FEE_PD".equals(sourceDbField) || "TRANSFER_FEE_NUMERICAL".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-active element to 'yes' for field: " + sourceDbField);
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimRenewal.xml") || sourcePath.contains("fimRenewal_copy.xml"))){
                    if("FIM_CB_CURRENT_STATUS".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-active element to 'yes' for field: " + sourceDbField);
                    }
                }

                // Special handling for ST_ID - always set is-active to "yes"
                if ("ST_ID".equals(sourceDbField)) {
                    String currentIsActive = XmlUtil.getValue(targetField, "is-active").trim().toLowerCase();
                    if (!"yes".equals(currentIsActive)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Set is-active element with value 'yes' to ST_ID field (special handling for ST_ID)");
                    }
                }

                // Add is-non-editable tag for STORE_STATUS field
                if ("STORE_STATUS".equals(sourceDbField)) {
                    XmlUtil.replaceOrInsertChild(targetField, "is-non-editable", "true");
                    System.out.println("Added is-non-editable 'true' to db-field '" + sourceDbField + "' (special handling for STORE_STATUS)");
                }
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

    public static Map<String, Element> readTabularSectionMappings(String xmlFilePath) {
        Map<String, Element> tabularSectionElements = new HashMap<>();

        try {
            Document tabularSectionDoc = XmlUtil.loadXmlDocument(xmlFilePath);

            NodeList tableMappings = tabularSectionDoc.getElementsByTagName("table-mapping");

            for (int i = 0; i < tableMappings.getLength(); i++) {
                Element element = (Element) tableMappings.item(i);
                String tableAnchor = element.getAttribute("table-anchor");
                if (tableAnchor == null || tableAnchor.isEmpty()) continue;
            
                tabularSectionElements.put(tableAnchor, element);
            }
            
        } catch (Exception e) {
            System.err.println("Error reading tabular section mappings: " + e.getMessage());
        }

        return tabularSectionElements;
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

            // add custom training module entry
            boolean customAdded = addCustomTrainingModule(targetDoc, targetRoot, targetTabModuleElements);
            updated = updated || customAdded;

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

    public static void processTabularSectionMappingsXml(Map<String, Element> sourceTabularSectionElements, Map<String, Element> targetTabularSectionElements, String sourceTabularSectionPath, String targetTabularSectionPath) {
        try {

            Document targetDoc = XmlUtil.loadXmlDocument(targetTabularSectionPath);
            Element targetRoot = targetDoc.getDocumentElement();
            boolean updated = false;

            for (Map.Entry<String, Element> entry : sourceTabularSectionElements.entrySet()) {
                String tableAnchor = entry.getKey();
                Element sourceTabularElement = entry.getValue();

                if (!targetTabularSectionElements.containsKey(tableAnchor)) {
                    Node importedNode = targetDoc.importNode(sourceTabularElement, true);
                    targetRoot.appendChild(importedNode);
                    updated = true;
                    System.out.println("Added missing <table-mapping> with table-anchor: " + tableAnchor);
                }
            }

            // add custom store timings entry
            boolean customAdded = addCustomStoreTimingsEntry(targetDoc, targetRoot, targetTabularSectionElements);
            updated = updated || customAdded;

            if (updated) {
                XmlUtil.saveXmlDocument(targetDoc, targetTabularSectionPath); // Overwrite or write to new file
                System.out.println("Target tabularSectionMappings.xml updated with new entries.");
            } else {
                System.out.println("No updates required. All table-mappings are already present.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to sync table-mappings: " + e.getMessage());
        }
    }

    private static boolean addCustomTrainingModule(Document targetDoc, Element targetRoot,
                                               Map<String, Element> targetTabModuleElements) {
        String trainingDbTable = "_TRAINING_1851313017";

        if (targetTabModuleElements.containsKey(trainingDbTable)) {
            System.out.println("Custom <module-tab> entry already exists for training.");
            return false;
        }

        Element customModule = targetDoc.createElement("module-tab");
        customModule.setAttribute("addMore", "true");
        customModule.setAttribute("builderFormId", "1851313017");
        customModule.setAttribute("condition", "dataPresent");
        customModule.setAttribute("db-table", trainingDbTable);
        customModule.setAttribute("fileLocation", "tables/buildertabs/training1851313017.xml");
        customModule.setAttribute("href", "/moduleCustomTab");
        customModule.setAttribute("is-active", "Y");
        customModule.setAttribute("is-exportable", "true");
        customModule.setAttribute("module", "fim");
        customModule.setAttribute("path", "/moduleCustomTab,/addModuleCustomTab");
        customModule.setAttribute("privilegeUrl", "/moduleCustomTab");
        customModule.setAttribute("submodule", "franchisee");
        customModule.setAttribute("tab-display", "TRAINING");
        customModule.setAttribute("tab-name", "training1851313017");
        customModule.setAttribute("tab-row", "1");
        customModule.setAttribute("tabOrder", "28");
        customModule.setAttribute("tableAnchor", "training1851313017");
        customModule.setAttribute("viewRoles", "");
        customModule.setAttribute("writeRoles", "");

        targetRoot.appendChild(customModule);
        System.out.println("Added custom <module-tab> entry for training.");
        return true;
    }

    private static boolean addCustomStoreTimingsEntry(Document targetDoc, Element targetRoot,
                                                   Map<String, Element> targetTabularSectionElements) {
        String storeTimingsTableAnchor = "storetimings1120613317";

        if (targetTabularSectionElements.containsKey(storeTimingsTableAnchor)) {
            System.out.println("Custom <table-mapping> entry already exists for store timings.");
            return false;
        }

        Element customMapping = targetDoc.createElement("table-mapping");
        customMapping.setAttribute("filelocation", "tables/buildertabs/storetimings1120613317.xml");
        customMapping.setAttribute("table-anchor", storeTimingsTableAnchor);

        targetRoot.appendChild(customMapping);
        System.out.println("Added custom <table-mapping> entry for store timings.");
        return true;
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

    public static String generateInsertQuery(String targetKeyPath, String filePath, String module, Set<String> underscoreFieldsSet) throws Exception {
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

            // Query to update all the fields with underscore
            List<String> pendingFields = new ArrayList<>();
            for (String fieldName : underscoreFieldsSet) {
                if (!processedUnderscoreFields.contains(fieldName)) {
                    pendingFields.add(fieldName);
                }
            }

            for (String fieldName : pendingFields) {
                String newField = "_" + fieldName;
                query.append(System.lineSeparator());
                query.append("UPDATE SUMMARY_DISPLAY SET FIELD_NAME = REPLACE(FIELD_NAME, '" + fieldName + "', '" + newField + "') WHERE FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE SUMMARY_DISPLAY SET CUSTOM_FIELD_NAME = REPLACE(CUSTOM_FIELD_NAME, '" + fieldName + "', '" + newField + "') WHERE CUSTOM_FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE FORM_FIELD_ACCESS_MAPPING SET FIELD_NAME = REPLACE(FIELD_NAME, '" + fieldName + "', '" + newField + "') WHERE FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE FIM_BUILDER_MASTER_DATA SET FIELD_NAME = REPLACE(FIELD_NAME, '" + fieldName + "', '" + newField + "') WHERE FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE TRIGGER_EVENT SET FIELD_NAME = REPLACE(FIELD_NAME, '" + fieldName + "', '" + newField + "') WHERE FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE TRIGGER_EVENT SET DB_FIELD_NAME = REPLACE(DB_FIELD_NAME, '" + fieldName.toUpperCase() + "', '" + newField.toUpperCase() + "') WHERE DB_FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE TABULAR_SECTION_DISPLAY_COLUMN SET FIELD_NAME = REPLACE(FIELD_NAME, '" + fieldName + "', '" + newField + "') WHERE TABLE_NAME LIKE '%" + xmlKey + "%' AND FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE TABULAR_SECTION_DISPLAY_COLUMN SET DISPLAY_VALUE = REPLACE(DISPLAY_VALUE, '" + fieldName.toUpperCase() + "', '" + newField.toUpperCase() + "') WHERE TABLE_NAME LIKE '%" + xmlKey + "%' AND DISPLAY_VALUE NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE SMART_GROUP_CRITERIA SET FIELD_NAME = REPLACE(FIELD_NAME, '" + fieldName + "', '" + newField + "') WHERE FIELD_NAME NOT LIKE '_%';");
                query.append(System.lineSeparator());
                query.append("UPDATE CUSTOM_REPORT SET");
                query.append("  CUSTOM_REPORT_SELECT_FIELDS = REPLACE(CUSTOM_REPORT_SELECT_FIELDS, '" + fieldName + "', '" + newField + "'),");
                query.append("  CUSTOM_REPORT_WHERE_FIELDS = REPLACE(CUSTOM_REPORT_WHERE_FIELDS, '" + fieldName + "', '" + newField + "'),");
                query.append("  CUSTOM_REPORT_SELECT_FIELDS_WITH_TABLES = REPLACE(CUSTOM_REPORT_SELECT_FIELDS_WITH_TABLES, '#####" + fieldName + "', '#####" + newField + "')");
                query.append(" WHERE CUSTOM_REPORT_SELECT_FIELDS NOT LIKE '_%' AND CUSTOM_REPORT_WHERE_FIELDS NOT LIKE '_%' AND CUSTOM_REPORT_SELECT_FIELDS_WITH_TABLES NOT LIKE '_%';");

                processedUnderscoreFields.add(fieldName);
            }

            String insertQuery = query.toString();

            System.out.println("Generated Query: " + insertQuery);

            return insertQuery;
        } catch (Exception e) {
            System.out.println("Error generating insert query: " + e.getMessage());
            return null;
        }
    }

    public static String getSpecificXmlQuery(String xmlKey) {
        StringBuilder query = new StringBuilder();
        if(xmlKey.equals("fimEntityDetail") || xmlKey.equals("fimEntityDetail_copy")) {
            query.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
            query.append("NULL, ");
            if(xmlKey.equals("fimEntityDetail")) {
                query.append("'fimEntityDetail.xml', ");
                query.append("'fimEntityDetail', ");
                query.append("'fim', ");
                query.append("'/tables/fim/fimEntityDetail.xml', ");
            }
            else if(xmlKey.equals("fimEntityDetail_copy")) {
                query.append("'fimEntityDetail_copy.xml', ");
                query.append("'fimEntityDetail_copy', ");
                query.append("'fim', ");
                query.append("'/tables/fim/fimEntityDetail_copy.xml', ");
            }

            String xmlData;
            try {
                xmlData = Files.readString(Paths.get("src/main/resources/requiredXml/fimEntityDetail.xml"));
            } catch (IOException e) {
                xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><table></table>";
            }

            // Escape single quotes in XML so it can be safely inserted
            xmlData = xmlData.replace("'", "''");

            query.append("'" + xmlData + "', ");
            query.append("NOW());"); // Assuming LAST_MODIFIED is a timestamp
        }else if("fimTraining".equals(xmlKey) || "fimTraining_copy".equals(xmlKey)){
            query.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
            query.append("NULL, ");
            if(xmlKey.equals("fimTraining")) {
                query.append("'training1851313017.xml', ");
                query.append("'training1851313017', ");
                query.append("'buildertabs', ");
                query.append("'/tables/buildertabs/training1851313017.xml', ");
            }
            else if(xmlKey.equals("fimTraining_copy")) {
                query.append("'training1851313017_copy.xml', ");
                query.append("'training1851313017_copy', ");
                query.append("'buildertabs', ");
                query.append("'/tables/buildertabs/training1851313017_copy.xml', ");
            }

            String xmlData;
            try {
                xmlData = Files.readString(Paths.get("src/main/resources/requiredXml/fimTraining.xml"));
            } catch (IOException e) {
                xmlData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><table></table>";
            }

            // Escape single quotes in XML so it can be safely inserted
            xmlData = xmlData.replace("'", "''");

            query.append("'" + xmlData + "', ");
            query.append("NOW());"); // Assuming LAST_MODIFIED is a timestamp
        }else if("franchisees".equals(xmlKey) || "franchisees_copy".equals(xmlKey)){
            query.append("DELETE FROM FIM_BUILDER_MASTER_DATA WHERE FIELD_NAME='_areaManager';");
            query.append(System.lineSeparator());
            query.append("SET @A:=0;");
            query.append(System.lineSeparator());
            query.append("INSERT INTO FIM_BUILDER_MASTER_DATA(FIELD_ID,FIELD_NAME,OPTION_ID,OPTION_VALUE,TABLE_ANCHOR,IS_ACTIVE,DEPENDENT_VALUE,ORDER_NO) ");
            query.append("SELECT '0','_areaManager',@A:=@A+1,ROLE_ID,'franchisees','Y',NULL,@A:=@A+1 FROM ROLE WHERE ROLE_ID IN (SELECT ROLE_ID FROM USER_ROLES WHERE USER_NO IN (SELECT USER_NO FROM USERS WHERE IS_AREA_MANAGER='Y' AND STATUS=1 AND IS_DELETED='N'));");
            query.append(System.lineSeparator());
            query.append("ALTER TABLE FRANCHISEE MODIFY AREA_MANAGER TEXT;");
            query.append(System.lineSeparator());
        }
        return query.toString();
    }

    public static void addStoreTimingsHeader(Document targetDoc, Element targetParent) {
        Element header = targetDoc.createElement("header");
        header.setAttribute("name", "bSec_storetimings1282868853");
        header.setAttribute("order", "5");
        header.setAttribute("value", "Store Timings");
        header.appendChild(createElement(targetDoc, "type", "0"));
        header.appendChild(createElement(targetDoc, "section", "bSec_storetimings1282868853"));
        header.appendChild(createElement(targetDoc, "is-build-section", "false"));
        header.appendChild(createElement(targetDoc, "tabular-section-table-anchor", "storehoursnd21214306162"));
        header.appendChild(createElement(targetDoc, "tabular-section-db-table", "_STOREHOURSND2_1214306162"));
        header.appendChild(createElement(targetDoc, "is-tabular-section", "yes"));
        targetParent.appendChild(header);
    }

    public static void addStoreTimingsForeignTable(Document targetDoc, Element targetParent) {
        Element foreignTable = targetDoc.createElement("foreign-table");
        foreignTable.setAttribute("name", "storehoursnd21214306162");
        foreignTable.setAttribute("table-export", "true");
    
        Element linkField1 = targetDoc.createElement("link-field");
        linkField1.setAttribute("foreignField", "tabPrimaryId");
        linkField1.setAttribute("thisField", "franchiseeNo");
        foreignTable.appendChild(linkField1);
    
        Element linkField2 = targetDoc.createElement("link-field");
        linkField2.setAttribute("foreignField", "entityID");
        linkField2.setAttribute("thisField", "entityID");
        foreignTable.appendChild(linkField2);
    
        targetParent.appendChild(foreignTable);
    }

    public static Element createElement(Document doc, String name, String value) {
        Element element = doc.createElement(name);
        element.setTextContent(value);
        return element;
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

    public static HashSet<String> getRequiredKeySet(){
        return new HashSet<>(Arrays.asList("address","address_copy","areaCall","areaCall_copy","areaContract","areaContractExport","areaContractExport_copy","areaContract_copy","areaEntityDetail","areaEntityDetailExport","areaEntityDetailExport_copy","areaEntityDetail_copy","areaEvents","areaEvents_copy","areaFinancial","areaFinancialExport","areaFinancialExport_copy","areaFinancial_copy","areaGuarantor","areaGuarantorExport","areaGuarantorExport_copy","areaGuarantor_copy","areaInfo","areaInfo_copy","areaInsurance","areaInsuranceExport","areaInsuranceExport_copy","areaInsurance_copy","areaLegalViolation","areaLegalViolation_copy","areaLender","areaLenderExport","areaLenderExport_copy","areaLender_copy","areaLicenseAgreement","areaLicenseAgreementExport","areaLicenseAgreementExport_copy","areaLicenseAgreement_copy","areaMarketing","areaMarketingExport","areaMarketingExport_copy","areaMarketing_copy","areaMysteryShopper","areaMysteryShopper_copy","areaOwners","areaOwners_copy","areaQa","areaQaExport","areaQaExport_copy","areaQa_copy","areaRealEstate","areaRealEstateExport","areaRealEstateExport_copy","areaRealEstate_copy","areaRenewal","areaRenewal_copy","areaTasks","areaTasks_copy","areaTerritory","areaTerritoryExport","areaTerritoryExport_copy","areaTerritory_copy","areaTraining","areaTrainingExport","areaTrainingExport_copy","areaTraining_copy","areaUsers","areaUsers_copy","areas","areas_copy","callStatus","callStatus_copy","callType_copy","centerInfoDisplay","centerInfoDisplay_copy","entityCall","entityCall_copy","entityDisplayDetail","entityDisplayDetail_copy","externalWebFormsApproval","externalWebFormsApproval_copy","fimAddress","fimAddressExport","fimAddressExport_copy","fimAddress_copy","fimAgreementVersionsExport","fimAgreementVersionsExport_copy","fimAreaRemarks","fimAreaRemarks_copy","fimBrandMapping","fimBrandMapping_copy","fimBuilderField","fimBuilderField_copy","fimCampaign","fimCampaignEmailCampaign","fimCampaignEmailCampaign_copy","fimCampaignTemplates","fimCampaignTemplates_copy","fimCampaign_copy","fimCapturePopServer","fimCapturePopServer_copy","fimComplaint","fimComplaintExport","fimComplaintExport_copy","fimComplaint_copy","fimConfigureOptOutMessage","fimConfigureOptOutMessage_copy","fimContract","fimContractAdditional","fimContractAdditional_copy","fimContractExport","fimContractExport_copy","fimContract_copy","fimCustomTab","fimCustomTabFields","fimCustomTabFields_copy","fimCustomTabSections","fimCustomTabSections_copy","fimCustomTab_copy","fimDocuments","fimDocuments_copy","fimEmployees","fimEmployeesExport","fimEmployeesExport_copy","fimEmployeesMapping","fimEmployeesMappingExport","fimEmployeesMappingExport_copy","fimEmployeesMapping_copy","fimEmployees_copy","fimEntityDetail","fimEntityDetailExport","fimEntityDetailExport_copy","fimEntityDetail_copy","fimEntityLocationMapping","fimEntityLocationMapping_copy","fimEntityOwnerMapping","fimEntityOwnerMapping_copy","fimEvents","fimEvents_copy","fimExternalMail","fimExternalMail_copy","fimFinancial","fimFinancialExport","fimFinancialExport_copy","fimFinancialIfFinancialsExport","fimFinancialIfFinancialsExport_copy","fimFinancial_copy","fimFranchiseAgreementVersionsExport","fimFranchiseAgreementVersionsExport_copy","fimFranchiseeEmail","fimFranchiseeEmail_copy","fimGroups","fimGroupsArchived","fimGroupsArchived_copy","fimGroups_copy","fimGuarantor","fimGuarantorExport","fimGuarantorExport_copy","fimGuarantor_copy","fimInsurance","fimInsuranceExport","fimInsuranceExport_copy","fimInsurance_copy","fimLegalViolation","fimLegalViolation_copy","fimLender","fimLenderExport","fimLenderExport_copy","fimLender_copy","fimLicenseAgreement","fimLicenseAgreementExport","fimLicenseAgreementExport_copy","fimLicenseAgreement_copy","fimMarketing","fimMarketingExport","fimMarketingExport_copy","fimMarketing_copy","fimMuContract","fimMuContractExport","fimMuContractExport_copy","fimMuContract_copy","fimMuDocuments","fimMuDocuments_copy","fimMuEntityDetail","fimMuEntityDetailExport","fimMuEntityDetailExport_copy","fimMuEntityDetail_copy","fimMuEvents","fimMuEvents_copy","fimMuInfo","fimMuInfo_copy","fimMuLegalViolation","fimMuLegalViolation_copy","fimMuLicenseAgreement","fimMuLicenseAgreementExport","fimMuLicenseAgreementExport_copy","fimMuLicenseAgreement_copy","fimMuMarketing","fimMuMarketingExport","fimMuMarketingExport_copy","fimMuMarketing_copy","fimMuOtherAddress","fimMuOtherAddressExport","fimMuOtherAddressExport_copy","fimMuOtherAddress_copy","fimMuOwners","fimMuOwnersExport","fimMuOwnersExport_copy","fimMuOwners_copy","fimMuRealEstate","fimMuRealEstateExport","fimMuRealEstateExport_copy","fimMuRealEstate_copy","fimMuRemarks","fimMuRemarks_copy","fimMuTerritory","fimMuTerritoryExport","fimMuTerritoryExport_copy","fimMuTerritory_copy","fimMysteryShopper","fimMysteryShopper_copy","fimOwners","fimOwnersExport","fimOwnersExport_copy","fimOwners_copy","fimPicture","fimPictureExport","fimPictureExport_copy","fimPicture_copy","fimQa","fimQaExport","fimQaExport_copy","fimQa_copy","fimReacquiring","fimReacquiring_copy","fimRealEstate","fimRealEstateExport","fimRealEstateExport_copy","fimRealEstate_copy","fimRenewal","fimRenewal_copy","fimSCFranchiseToDoList","fimSCFranchiseToDoList_copy","fimSCToDoList","fimSCToDoList_copy","fimTasks","fimTasks_copy","fimTemplates","fimTemplates_copy","fimTermination","fimTerminationExport","fimTerminationExport_copy","fimTermination_copy","fimTerritory","fimTerritoryExport","fimTerritoryExport_copy","fimTerritory_copy","fimTraining","fimTrainingCourseExport","fimTrainingCourseExport_copy","fimTrainingParticipantExport","fimTrainingParticipantExport_copy","fimTrainingQuizParticipantExport","fimTrainingQuizParticipantExport_copy","fimTrainingUsersExport","fimTrainingUsersExport_copy","fimTraining_copy","fimTransfer","fimTransferExport","fimTransferExport_copy","fimTransferStatus","fimTransferStatus_copy","fimTransfer_copy","fimUsers","fimUsers_copy","fimfranchiseeMapping","fimfranchiseeMapping_copy","fimfranchiseeRemarks","fimfranchiseeRemarks_copy","franchiseeCall","franchiseeCall_copy","franchiseeExport","franchiseeExport_copy","franchiseeLocalListings","franchiseeLocalListings_copy","franchiseeMailmergeTemplateRel","franchiseeMailmergeTemplateRel_copy","franchisees","franchiseesExport","franchiseesExport_copy","franchisees_copy","fsFranchiseDevelopment","fsFranchiseDevelopment_copy","fsFranchiseeQualification","fsFranchiseeQualification_copy","fsLeadBusinessProfile","fsLeadBusinessProfile_copy","fsLeadCompliance","fsLeadComplianceAdditional","fsLeadComplianceAdditional_copy","fsLeadCompliance_copy","fsLeadDetails","fsLeadDetailsExport","fsLeadDetailsExport_copy","fsLeadDetails_copy","fsLeadPersonalProfile","fsLeadPersonalProfile_copy","fsLeadQualification","fsLeadQualificationDetail","fsLeadQualificationDetail_copy","fsLeadQualification_copy","fsLeadRating","fsLeadRating_copy","fsLeadRealEstate","fsLeadRealEstate_copy","fsLeadSMS","fsLeadSMS_copy","fsLeadSchedule","fsLeadScheduleVisitors","fsLeadScheduleVisitors_copy","fsLeadSchedule_copy","fsSiteLocation","fsSiteLocation_copy","fsSmsTemplates","fsSmsTemplates_copy","fsSubscriptionLogs","fsSubscriptionLogs_copy","fsSubscriptionMailData","fsSubscriptionMailData_copy","fsSubscriptionSMSData","fsSubscriptionSMSData_copy","fsTaskTriggers","fsTaskTriggers_copy","fsTasks","fsTasks_copy","fsVisitTaskMapping","fsVisitTaskMapping_copy","fsleadCall","fsleadCall_copy","muCall","muCall_copy","muDetails","muDetailsExport","muDetailsExport_copy","muDetails_copy","muFimOwners","muFimOwners_copy","muFinancial","muFinancialExport","muFinancialExport_copy","muFinancial_copy","muGuarantor","muGuarantorExport","muGuarantorExport_copy","muGuarantor_copy","muInsurance","muInsuranceExport","muInsuranceExport_copy","muInsurance_copy","muLender","muLenderExport","muLenderExport_copy","muLender_copy","muMysteryShopper","muMysteryShopper_copy","muOutlookMailAttachments","muOutlookMailAttachments_copy","muOutlookMails","muOutlookMailsExport","muOutlookMailsExport_copy","muOutlookMails_copy","muOwners","muOwnersExport","muOwnersExport_copy","muOwners_copy","muQa","muQaExport","muQaExport_copy","muQa_copy","muRenewal","muRenewal_copy","owners","ownersExport","ownersExport_copy","owners_copy","tabularSectionMappings"));
    }
}