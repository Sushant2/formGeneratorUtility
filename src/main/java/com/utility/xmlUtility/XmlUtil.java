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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlUtil {

    /** HTML5-style doctype is not allowed inside element content in XML; MBE exports embed it in {@code display-name}. */
    private static final Pattern EMBEDDED_HTML_DOCTYPE = Pattern.compile("<!DOCTYPE\\s+html\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private static final Pattern DISPLAY_NAME_BLOCK = Pattern.compile("<display-name>\\s*(.*?)\\s*</display-name>", Pattern.DOTALL);

    private static final Pattern NUMERIC_CHAR_REF_DECIMAL = Pattern.compile("&#([0-9]{1,7});");

    private static final Pattern NUMERIC_CHAR_REF_HEX = Pattern.compile("&#x([0-9a-fA-F]{1,6});");

    private static final Pattern NAMED_HTML_ENTITY = Pattern.compile("&([a-zA-Z][a-zA-Z0-9]*);");

    /**
     * {@code display-name} sometimes stores HTML as a single text node (e.g. after {@code &lt;...&gt;} is parsed),
     * so there are no child elements — only this pattern can recover the label.
     */
    private static final Pattern STRONG_IN_DISPLAY_NAME_TEXT =
            Pattern.compile("<strong\\b[^>]*>(.*?)</strong>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    /** HTML 4 named entities often found in MBE labels (XML has no DTD for these). */
    private static final Map<String, String> HTML_NAMED_ENTITIES_TO_CHAR = new HashMap<>();

    static {
        Object[][] pairs = new Object[][] {
            { "nbsp", "\u00A0" }, { "iexcl", "\u00A1" }, { "cent", "\u00A2" }, { "pound", "\u00A3" }, { "curren", "\u00A4" },
            { "yen", "\u00A5" }, { "brvbar", "\u00A6" }, { "sect", "\u00A7" }, { "uml", "\u00A8" }, { "copy", "\u00A9" },
            { "ordf", "\u00AA" }, { "laquo", "\u00AB" }, { "not", "\u00AC" }, { "shy", "\u00AD" }, { "reg", "\u00AE" },
            { "macr", "\u00AF" }, { "deg", "\u00B0" }, { "plusmn", "\u00B1" }, { "sup2", "\u00B2" }, { "sup3", "\u00B3" },
            { "acute", "\u00B4" }, { "micro", "\u00B5" }, { "para", "\u00B6" }, { "middot", "\u00B7" }, { "cedil", "\u00B8" },
            { "sup1", "\u00B9" }, { "ordm", "\u00BA" }, { "raquo", "\u00BB" }, { "frac14", "\u00BC" }, { "frac12", "\u00BD" },
            { "frac34", "\u00BE" }, { "iquest", "\u00BF" }, { "Agrave", "\u00C0" }, { "Aacute", "\u00C1" }, { "Acirc", "\u00C2" },
            { "Atilde", "\u00C3" }, { "Auml", "\u00C4" }, { "Aring", "\u00C5" }, { "AElig", "\u00C6" }, { "Ccedil", "\u00C7" },
            { "Egrave", "\u00C8" }, { "Eacute", "\u00C9" }, { "Ecirc", "\u00CA" }, { "Euml", "\u00CB" }, { "Igrave", "\u00CC" },
            { "Iacute", "\u00CD" }, { "Icirc", "\u00CE" }, { "Iuml", "\u00CF" }, { "ETH", "\u00D0" }, { "Ntilde", "\u00D1" },
            { "Ograve", "\u00D2" }, { "Oacute", "\u00D3" }, { "Ocirc", "\u00D4" }, { "Otilde", "\u00D5" }, { "Ouml", "\u00D6" },
            { "times", "\u00D7" }, { "Oslash", "\u00D8" }, { "Ugrave", "\u00D9" }, { "Uacute", "\u00DA" }, { "Ucirc", "\u00DB" },
            { "Uuml", "\u00DC" }, { "Yacute", "\u00DD" }, { "THORN", "\u00DE" }, { "szlig", "\u00DF" }, { "agrave", "\u00E0" },
            { "aacute", "\u00E1" }, { "acirc", "\u00E2" }, { "atilde", "\u00E3" }, { "auml", "\u00E4" }, { "aring", "\u00E5" },
            { "aelig", "\u00E6" }, { "ccedil", "\u00E7" }, { "egrave", "\u00E8" }, { "eacute", "\u00E9" }, { "ecirc", "\u00EA" },
            { "euml", "\u00EB" }, { "igrave", "\u00EC" }, { "iacute", "\u00ED" }, { "icirc", "\u00EE" }, { "iuml", "\u00EF" },
            { "eth", "\u00F0" }, { "ntilde", "\u00F1" }, { "ograve", "\u00F2" }, { "oacute", "\u00F3" }, { "ocirc", "\u00F4" },
            { "otilde", "\u00F5" }, { "ouml", "\u00F6" }, { "divide", "\u00F7" }, { "oslash", "\u00F8" }, { "ugrave", "\u00F9" },
            { "uacute", "\u00FA" }, { "ucirc", "\u00FB" }, { "uuml", "\u00FC" }, { "yacute", "\u00FD" }, { "thorn", "\u00FE" },
            { "yuml", "\u00FF" },
        };
        for (Object[] p : pairs) {
            HTML_NAMED_ENTITIES_TO_CHAR.put((String) p[0], (String) p[1]);
        }
    }
    
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

    private static String codePointToString(int code) {
        if (code < 0 || code > Character.MAX_CODE_POINT) {
            return "\uFFFD";
        }
        return code <= 0xFFFF ? String.valueOf((char) code) : new String(Character.toChars(code));
    }

    private static String unescapeHtmlInDisplayNameInner(String inner) {
        if (inner == null || inner.isEmpty()) {
            return inner;
        }
        Matcher dec = NUMERIC_CHAR_REF_DECIMAL.matcher(inner);
        StringBuffer sb1 = new StringBuffer();
        while (dec.find()) {
            int code = Integer.parseInt(dec.group(1));
            dec.appendReplacement(sb1, Matcher.quoteReplacement(codePointToString(code)));
        }
        dec.appendTail(sb1);
        inner = sb1.toString();

        Matcher hex = NUMERIC_CHAR_REF_HEX.matcher(inner);
        StringBuffer sb2 = new StringBuffer();
        while (hex.find()) {
            int code = Integer.parseInt(hex.group(1), 16);
            hex.appendReplacement(sb2, Matcher.quoteReplacement(codePointToString(code)));
        }
        hex.appendTail(sb2);
        inner = sb2.toString();

        Matcher named = NAMED_HTML_ENTITY.matcher(inner);
        StringBuffer sb3 = new StringBuffer();
        while (named.find()) {
            String name = named.group(1);
            if ("amp".equals(name) || "lt".equals(name) || "gt".equals(name) || "apos".equals(name) || "quot".equals(name)) {
                named.appendReplacement(sb3, Matcher.quoteReplacement(named.group(0)));
                continue;
            }
            String ch = HTML_NAMED_ENTITIES_TO_CHAR.get(name);
            if (ch == null) {
                named.appendReplacement(sb3, Matcher.quoteReplacement(named.group(0)));
            } else {
                named.appendReplacement(sb3, Matcher.quoteReplacement(ch));
            }
        }
        named.appendTail(sb3);
        return sb3.toString();
    }

    /**
     * MBE (and similar) table XML often contains invalid XML: {@code <!DOCTYPE html>} inside {@code display-name}
     * and HTML named entities ({@code &egrave;}) without a DTD. Sanitize so the JDK parser can load the document.
     */
    public static String sanitizeTableXmlForParsing(String raw) {
        if (raw == null || raw.isEmpty()) {
            return raw;
        }
        String withoutDoctype = EMBEDDED_HTML_DOCTYPE.matcher(raw).replaceAll("");
        Matcher m = DISPLAY_NAME_BLOCK.matcher(withoutDoctype);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String inner = unescapeHtmlInDisplayNameInner(m.group(1));
            String replacement = "<display-name>" + inner + "</display-name>";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static Document loadXmlDocument(String filePath) throws Exception {
        String raw = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        String sanitized = sanitizeTableXmlForParsing(raw);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(sanitized)));
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

    /**
     * Builder / form-generated columns use a leading underscore in {@code db-field}; system columns (e.g. {@code FRANCHISEE_NO}) do not.
     */
    public static boolean isBuilderStyleDbField(String dbField) {
        if (dbField == null) {
            return false;
        }
        String t = dbField.trim();
        return !t.isEmpty() && t.charAt(0) == '_';
    }

    public static void removeDirectChildTag(Element parent, String tagName) {
        if (parent == null) {
            return;
        }
        Element child = getDirectChildNode(parent, tagName);
        if (child != null) {
            parent.removeChild(child);
        }
    }

    /**
     * System {@code db-field} values must not carry {@code build-field}; removes the element if present.
     */
    public static void removeBuildFieldForSystemDbField(Element field, String dbFieldValue) {
        if (field == null || isBuilderStyleDbField(dbFieldValue)) {
            return;
        }
        removeDirectChildTag(field, "build-field");
    }

    /**
     * Copies {@code <jScriptFunct .../>} from the source {@code field} when present (replaces any existing on target).
     */
    /**
     * Undo SQL-style doubled single-quotes in JS attribute values (e.g. {@code getSourceDetails(''x'',null)}).
     */
    private static void normalizeJScriptFunctQuotedStrings(Element jScriptEl) {
        if (jScriptEl == null || !jScriptEl.hasAttributes()) {
            return;
        }
        NamedNodeMap attrs = jScriptEl.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node a = attrs.item(i);
            if (a.getNodeType() != Node.ATTRIBUTE_NODE) {
                continue;
            }
            String v = a.getNodeValue();
            if (v != null && v.contains("''")) {
                ((Attr) a).setValue(v.replace("''", "'"));
            }
        }
    }

    /**
     * Franchisee / store combo on external forms: {@code <source-field>true</source-field>}.
     */
    public static void syncSourceFieldForExternalForm(Element sourceField, Element targetField) {
        if (sourceField == null || targetField == null) {
            return;
        }
        Element src = getDirectChildNode(sourceField, "source-field");
        if (src != null) {
            replaceOrInsertChild(targetField, "source-field", src.getTextContent().trim());
            return;
        }
        String db = getValue(sourceField, "db-field").trim().toUpperCase(Locale.ROOT);
        String displayType = getDisplayType(sourceField);
        if ("FRANCHISEE_NO".equals(db) && "Combo".equalsIgnoreCase(displayType)) {
            replaceOrInsertChild(targetField, "source-field", "true");
        }
    }

    public static void syncJScriptFunctFromSource(Element sourceField, Element targetField, Document targetDoc) {
        if (sourceField == null || targetField == null || targetDoc == null) {
            return;
        }
        Element src = getDirectChildNode(sourceField, "jScriptFunct");
        if (src == null) {
            return;
        }
        removeDirectChildTag(targetField, "jScriptFunct");
        Element imported = (Element) targetDoc.importNode(src, true);
        normalizeJScriptFunctQuotedStrings(imported);
        targetField.appendChild(imported);
    }

    /**
     * First {@code display-name} under a field (direct child preferred).
     */
    public static Element getFirstDisplayNameElement(Element field) {
        if (field == null) {
            return null;
        }
        Element direct = getDirectChildNode(field, "display-name");
        if (direct != null) {
            return direct;
        }
        NodeList nl = field.getElementsByTagName("display-name");
        if (nl.getLength() > 0) {
            return (Element) nl.item(0);
        }
        return null;
    }

    /** Nearest ancestor {@code <table>} of a field, or null. */
    public static Element findAncestorTableElement(Element el) {
        Node n = el;
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) n;
                if ("table".equals(e.getNodeName())) {
                    return e;
                }
            }
            n = n.getParentNode();
        }
        return null;
    }

    /**
     * Store / franchisee id column: should not get a default {@code triggerFormName} (matches external-form exports).
     */
    public static boolean isPrimaryTableIdField(Element field, Element tableEl) {
        if (field == null) {
            return false;
        }
        String db = getValue(field, "db-field").trim().toUpperCase(Locale.ROOT);
        if ("FRANCHISEE_NO".equals(db)) {
            return true;
        }
        if (tableEl == null) {
            return false;
        }
        String idField = getValue(tableEl, "id-field").trim();
        if (idField.isEmpty()) {
            return false;
        }
        String fn = getValue(field, "field-name").trim();
        if (fn.isEmpty()) {
            return false;
        }
        String fnNorm = fn.startsWith("_") ? fn.substring(1) : fn;
        return idField.equals(fn) || idField.equals(fnNorm);
    }

    /**
     * Default for {@code triggerFormName} when not set on a field: optional {@code <trigger-form-name>} on the table
     * (must match the form builder / palette name in FC), else {@code table-display-name}.
     */
    public static String getDefaultTriggerFormNameForTable(Element tableEl) {
        if (tableEl == null) {
            return "";
        }
        String explicit = getValue(tableEl, "trigger-form-name").trim();
        if (!explicit.isEmpty()) {
            return explicit;
        }
        return getValue(tableEl, "table-display-name").trim();
    }

    /**
     * Keeps {@code summary} / {@code triggerFormName} on {@code targetField} in sync with {@code sourceField}.
     * When {@code triggerFormName} is omitted on the source but {@code summary="true"}, sets it from
     * {@link #getDefaultTriggerFormNameForTable} (except for the primary id field).
     */
    public static void syncSummaryAndTriggerFormAttributes(Element sourceField, Element targetField) {
        if (sourceField == null || targetField == null) {
            return;
        }
        Element tableEl = findAncestorTableElement(sourceField);
        if (sourceField.hasAttribute("summary")) {
            targetField.setAttribute("summary", sourceField.getAttribute("summary"));
        }
        boolean summaryTrue = "true".equalsIgnoreCase(targetField.getAttribute("summary"));
        if (sourceField.hasAttribute("triggerFormName")) {
            targetField.setAttribute("triggerFormName", sourceField.getAttribute("triggerFormName"));
        } else if (summaryTrue && !isPrimaryTableIdField(sourceField, tableEl)) {
            String tdn = getDefaultTriggerFormNameForTable(tableEl);
            if (!tdn.isEmpty()) {
                targetField.setAttribute("triggerFormName", tdn);
            } else {
                targetField.removeAttribute("triggerFormName");
            }
        } else {
            targetField.removeAttribute("triggerFormName");
        }
    }

    /**
     * Keeps {@code isCurreny} in sync (platform spelling). Defaults to {@code false} when the source omits the tag.
     */
    public static void syncIsCurrenyFromSource(Element sourceField, Element targetField) {
        if (sourceField == null || targetField == null) {
            return;
        }
        String v = getValue(sourceField, "isCurreny").trim();
        if (v.isEmpty()) {
            v = "false";
        }
        replaceOrInsertChild(targetField, "isCurreny", v);
    }

    private static String stripXmlLikeTagsToSingleLine(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return s.replaceAll("(?s)<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * True when {@code display-name} text is HTML stored as character data (e.g. {@code &lt;html&gt;...} in the file),
     * not as real XML child elements.
     */
    private static boolean displayNameTextLooksLikeEmbeddedHtml(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String t = text.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        return lower.contains("<strong") || lower.startsWith("<html") || lower.startsWith("<!doctype");
    }

    private static String getPlainLabelFromHtmlishDisplayNameText(String text) {
        Matcher sm = STRONG_IN_DISPLAY_NAME_TEXT.matcher(text);
        if (sm.find()) {
            return stripXmlLikeTagsToSingleLine(sm.group(1)).trim();
        }
        if (displayNameTextLooksLikeEmbeddedHtml(text)) {
            return stripXmlLikeTagsToSingleLine(text);
        }
        return text.trim();
    }

    private static String getDescriptionFromHtmlishDisplayNameText(String full) {
        Matcher sm = STRONG_IN_DISPLAY_NAME_TEXT.matcher(full);
        if (!sm.find()) {
            return "undefined";
        }
        String titlePlain = stripXmlLikeTagsToSingleLine(sm.group(1)).trim();
        String after = full.substring(sm.end());
        String restPlain = stripXmlLikeTagsToSingleLine(after).trim();
        if (restPlain.isEmpty()) {
            return "undefined";
        }
        String fullPlain = stripXmlLikeTagsToSingleLine(full).trim();
        if (fullPlain.equals(titlePlain)) {
            return "undefined";
        }
        return restPlain;
    }

    private static boolean displayNameElementHasMarkup(Element displayNameEl) {
        if (displayNameEl == null) {
            return false;
        }
        NodeList children = displayNameEl.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            short t = children.item(i).getNodeType();
            if (t == Node.ELEMENT_NODE || t == Node.DOCUMENT_TYPE_NODE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Plain label text from a {@code display-name} element (e.g. first {@code strong}, or full text if HTML wrapper only).
     */
    public static String getPlainDisplayNameFromDisplayNameElement(Element displayNameEl) {
        if (displayNameEl == null) {
            return "";
        }
        NodeList strongs = displayNameEl.getElementsByTagName("strong");
        if (strongs.getLength() > 0) {
            return strongs.item(0).getTextContent().trim();
        }
        if (displayNameElementHasMarkup(displayNameEl)) {
            return displayNameEl.getTextContent().trim().replaceAll("\\s+", " ");
        }
        String rawText = displayNameEl.getTextContent().trim();
        if (displayNameTextLooksLikeEmbeddedHtml(rawText)) {
            return getPlainLabelFromHtmlishDisplayNameText(rawText);
        }
        return rawText;
    }

    /**
     * Helper / secondary text from HTML {@code display-name} (text beyond the first {@code strong}), or {@code undefined}.
     */
    public static String getDisplayDescriptionFromDisplayNameElement(Element displayNameEl) {
        if (displayNameEl == null) {
            return "undefined";
        }
        if (!displayNameElementHasMarkup(displayNameEl)) {
            String rawText = displayNameEl.getTextContent().trim();
            if (displayNameTextLooksLikeEmbeddedHtml(rawText)) {
                return getDescriptionFromHtmlishDisplayNameText(rawText);
            }
            return "undefined";
        }
        NodeList strongs = displayNameEl.getElementsByTagName("strong");
        String full = displayNameEl.getTextContent().trim().replaceAll("\\s+", " ");
        if (strongs.getLength() > 0) {
            String title = strongs.item(0).getTextContent().trim().replaceAll("\\s+", " ");
            if (full.equals(title)) {
                return "undefined";
            }
            if (full.startsWith(title)) {
                String rest = full.substring(title.length()).trim();
                return rest.isEmpty() ? "undefined" : rest;
            }
            int idx = full.indexOf(title);
            if (idx >= 0) {
                String rest = (full.substring(0, idx) + full.substring(idx + title.length())).trim().replaceAll("\\s+", " ");
                return rest.isEmpty() ? "undefined" : rest;
            }
        }
        return "undefined";
    }

    public static String getPlainDisplayName(Element field) {
        return getPlainDisplayNameFromDisplayNameElement(getFirstDisplayNameElement(field));
    }

    public static String getDisplayDescriptionFromField(Element field) {
        return getDisplayDescriptionFromDisplayNameElement(getFirstDisplayNameElement(field));
    }

    /**
     * Replaces HTML {@code display-name} with plain text and adds {@code display-description} (for source-style MBE XML).
     */
    public static void normalizeDisplayNameOnFieldElement(Element field) {
        if (field == null) {
            return;
        }
        Element dn = getFirstDisplayNameElement(field);
        if (dn != null && (displayNameElementHasMarkup(dn)
                || displayNameTextLooksLikeEmbeddedHtml(dn.getTextContent()))) {
            String plain = getPlainDisplayNameFromDisplayNameElement(dn);
            String desc = getDisplayDescriptionFromDisplayNameElement(dn);
            replaceOrInsertChild(field, "display-name", plain);
            replaceOrInsertChild(field, "display-description", desc);
        }
        removeBuildFieldForSystemDbField(field, getValue(field, "db-field"));
    }

    public static void syncDisplayDescriptionIfSourceHadMarkup(Element sourceField, Element targetField) {
        Element dn = getFirstDisplayNameElement(sourceField);
        if (dn != null && (displayNameElementHasMarkup(dn)
                || displayNameTextLooksLikeEmbeddedHtml(dn.getTextContent()))) {
            replaceOrInsertChild(targetField, "display-description", getDisplayDescriptionFromDisplayNameElement(dn));
        }
    }

    public static void updateTagsIfDiff(Element sourceField, Map<String, Element> targetFieldMap, Map<String, String> updatedHeaders, Document sourceDoc, Document targetDoc, String sourcePath) {
        String sourceDbField = XmlUtil.getValue(sourceField, "db-field").trim().toUpperCase();
        String sourceDisplayName = XmlUtil.getPlainDisplayName(sourceField).trim();
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
                    if("BUYER_EXISTING_OR_NEW_FRANCHISEE".equals(sourceDbField) || "FIRST_NAME".equals(sourceDbField) || "LAST_NAME".equals(sourceDbField) || "FRANCHISE_OWNER_ID".equals(sourceDbField) || "TRANSFER_FEE_PD".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "no");
                        System.out.println("Updated is-active element to 'no' for field: " + sourceDbField);
                    }
                    if("TRANSFER_FEE_NUMERICAL".equals(sourceDbField) || "AS_OF".equals(sourceDbField) || "_DATA_SCADENZA_CONTRATTO_981704876".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        System.out.println("Updated is-active and is-mandatory element to 'yes' and 'true' for field: " + sourceDbField);
                    }
                    if("AS_OF".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "As Of Transfer Date");
                        System.out.println("Updated display-name element to 'As Of Transfer Date' for field: " + sourceDbField);
                    }
                    if("CURRENT_STATUS".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "Transfer Current Status");
                        System.out.println("Updated display-name element to 'Transfer Current Status' for field: " + sourceDbField);
                    }
                    if("TRANSFER_FEE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "no");
                        System.out.println("Updated is-active element to 'no' for field: " + sourceDbField);
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimRenewal.xml") || sourcePath.contains("fimRenewal_copy.xml"))){
                    if("FIM_CB_CURRENT_STATUS".equals(sourceDbField) || "FIM_TT_RENEWAL_FEES".equals(sourceDbField) || "FIM_DD_AS_OF".equals(sourceDbField) || "FIM_DD_NEW_EXPIRATION_DATE".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-mandatory to 'true' and is-active to 'yes' for field: " + sourceDbField + " in fimRenewal.xml");
                    }
                    if("FIM_CB_CURRENT_STATUS".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "Renewal Current Status");
                        System.out.println("Updated display-name element to 'Renewal Current Status' for field: " + sourceDbField);
                    }
                    if("FIM_DD_AS_OF".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "As Of Renewal Date");
                        System.out.println("Updated display-name element to 'As Of Renewal Date' for field: " + sourceDbField);
                    }
                }

                if(sourcePath != null && (sourcePath.contains("franchisees.xml") || sourcePath.contains("franchisees_copy.xml"))){
                    // Special handling for STORE_OPENING_DATE as inactive
                    if("STORE_OPENING_DATE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "no");
                        System.out.println("Set is-active element with value 'no' to STORE_OPENING_DATE field");
                    }
                    if("LONGITUDE".equals(sourceBuildField) || "LATITUDE".equals(sourceBuildField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "false");
                        System.out.println("Set is-mandatory element with value 'false' to LONGITUDE and LATITUDE fields");
                    }
                    if("GRAND_STORE_OPENING_DATE".equals(sourceBuildField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        System.out.println("Set is-mandatory element with value 'true' to GRAND_STORE_OPENING_DATE field");
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimTermination.xml") || sourcePath.contains("fimTermination_copy.xml"))){
                    if("FIM_DD_APPROVED_DATE".equals(sourceDbField) || "FIM_DD_TERMINATED_DATE".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-mandatory to 'true' and is-active to 'yes' for field: " + sourceDbField + " in fimRenewal.xml");
                    }
                }
                
                // Add build-field tag (builder columns only; system db-fields such as FRANCHISEE_NO omit it)
                if (!sourceBuildField.isEmpty() && XmlUtil.isBuilderStyleDbField(sourceDbField)) {
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
                    if("LONGITUDE".equals(sourceDbField) || "LATITUDE".equals(sourceDbField) || "GRAND_STORE_OPENING_DATE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "false");
                        System.out.println("Set is-mandatory element with value 'false' to LONGITUDE and LATITUDE fields");
                    }
                    if("GRAND_STORE_OPENING_DATE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        System.out.println("Set is-mandatory element with value 'true' to GRAND_STORE_OPENING_DATE field");
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimTransfer.xml") || sourcePath.contains("fimTransfer_copy.xml"))){
                    // Special handling for TRANSFER_FEE_PD and TRANSFER_FEE_NUMERICAL - always set is-active to "yes"
                    if("TRANSFER_FEE_NUMERICAL".equals(sourceDbField) || "AS_OF".equals(sourceDbField) || "_DATA_SCADENZA_CONTRATTO_981704876".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        System.out.println("Updated is-active and is-mandatory element to 'yes' and 'true' for field: " + sourceDbField);
                    }
                    if("AS_OF".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "As Of Transfer Date");
                        System.out.println("Updated display-name element to 'As Of Transfer Date' for field: " + sourceDbField);
                    }
                    if("TRANSFER_DATE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "Transfer Completion Date");
                        System.out.println("Updated display-name element to 'Transfer Completion Date' for field: " + sourceDbField);
                    }
                    if("TRANSFER_FEE_PD".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-active element to 'yes' for field: " + sourceDbField);
                    }
                    if("CURRENT_STATUS".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "Transfer Current Status");
                        System.out.println("Updated display-name element to 'Transfer Current Status' for field: " + sourceDbField);
                    }
                    if("TRANSFER_FEE".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "no");
                        System.out.println("Updated is-active element to 'no' for field: " + sourceDbField);
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimRenewal.xml") || sourcePath.contains("fimRenewal_copy.xml"))){
                    if("FIM_CB_CURRENT_STATUS".equals(sourceDbField) || "FIM_TT_RENEWAL_FEES".equals(sourceDbField) || "FIM_DD_AS_OF".equals(sourceDbField) || "FIM_DD_NEW_EXPIRATION_DATE".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-mandatory to 'true' and is-active to 'yes' for field: " + sourceDbField + " in fimRenewal.xml");
                    }
                    if("FIM_CB_CURRENT_STATUS".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "Renewal Current Status");
                        System.out.println("Updated display-name element to 'Renewal Current Status' for field: " + sourceDbField);
                    }
                    if("FIM_DD_AS_OF".equals(sourceDbField)){
                        XmlUtil.replaceOrInsertChild(targetField, "display-name", "As Of Renewal Date");
                        System.out.println("Updated display-name element to 'As Of Renewal Date' for field: " + sourceDbField);
                    }
                }

                if(sourcePath != null && (sourcePath.contains("fimTermination.xml") || sourcePath.contains("fimTermination_copy.xml"))){
                    if("FIM_DD_APPROVED_DATE".equals(sourceDbField) || "FIM_DD_TERMINATED_DATE".equals(sourceDbField)) {
                        XmlUtil.replaceOrInsertChild(targetField, "is-mandatory", "true");
                        XmlUtil.replaceOrInsertChild(targetField, "is-active", "yes");
                        System.out.println("Updated is-mandatory to 'true' and is-active to 'yes' for field: " + sourceDbField + " in fimRenewal.xml");
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

            XmlUtil.syncJScriptFunctFromSource(sourceField, targetField, targetDoc);
            XmlUtil.syncSourceFieldForExternalForm(sourceField, targetField);
            XmlUtil.removeBuildFieldForSystemDbField(targetField, sourceDbField);
            XmlUtil.syncDisplayDescriptionIfSourceHadMarkup(sourceField, targetField);
            XmlUtil.syncSummaryAndTriggerFormAttributes(sourceField, targetField);
            XmlUtil.syncIsCurrenyFromSource(sourceField, targetField);
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

    /**
     * Ensures {@code form-meta-data} on the target table matches the source (tabs, audit id, etc.).
     * Needed when the target file already existed: {@link #copyTableContent} only runs for brand-new targets.
     *
     * @return true if the target document was modified
     */
    public static boolean syncFormMetaDataFromSource(Document sourceDoc, Document targetDoc) {
        Element sourceTable = (Element) sourceDoc.getElementsByTagName("table").item(0);
        Element targetTable = (Element) targetDoc.getElementsByTagName("table").item(0);
        if (sourceTable == null || targetTable == null) {
            return false;
        }
        Element sourceMeta = getDirectChildNode(sourceTable, "form-meta-data");
        if (sourceMeta == null) {
            return false;
        }
        Element existingMeta = getDirectChildNode(targetTable, "form-meta-data");
        if (existingMeta != null) {
            targetTable.removeChild(existingMeta);
        }
        Node imported = targetDoc.importNode(sourceMeta, true);
        Element insertBeforeAnchor = getDirectChildNode(targetTable, "table-header-map");
        if (insertBeforeAnchor == null) {
            insertBeforeAnchor = getDirectChildNode(targetTable, "foreign-tables");
        }
        if (insertBeforeAnchor == null) {
            insertBeforeAnchor = getDirectChildNode(targetTable, "id-field");
        }
        if (insertBeforeAnchor == null) {
            NodeList children = targetTable.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node n = children.item(i);
                if (n.getNodeType() == Node.ELEMENT_NODE && "field".equals(n.getNodeName())) {
                    insertBeforeAnchor = (Element) n;
                    break;
                }
            }
        }
        if (insertBeforeAnchor != null) {
            targetTable.insertBefore(imported, insertBeforeAnchor);
        } else {
            targetTable.appendChild(imported);
        }
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