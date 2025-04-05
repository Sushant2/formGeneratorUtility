package com.utility.xmlUtility;
 
import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
 
@Service
public class XmlService {
    public void processXmlFiles(String sourcePath, String targetPath, String missingFieldsPath, String missingHeadersPath, String missingForeignTablesPath) {

        try {
            Document sourceDoc = XmlUtil.loadXmlDocument(sourcePath);
            Document targetDoc = XmlUtil.loadXmlDocument(targetPath);

            // Extract existing elements from target XML
            Set<String> targetHeaders = XmlUtil.extractElements(targetDoc, "header");
            Set<String> targetForeignTables = XmlUtil.extractElements(targetDoc, "foreign-table");
            Set<String> targetDbFields = XmlUtil.extractElements(targetDoc, "db-field");

            NodeList sourceHeaders = sourceDoc.getElementsByTagName("header");
            NodeList sourceForeignTables = sourceDoc.getElementsByTagName("foreign-table");
            NodeList sourceFields = sourceDoc.getElementsByTagName("field");
            
            // Create separate XML documents for missing fields, headers, and foreign tables
            Document missingHeadersDoc = XmlUtil.createNewXmlDocument("missingHeaders");
            Document missingForeignTablesDoc = XmlUtil.createNewXmlDocument("missingForeignTables");
            Document missingFieldsDoc = XmlUtil.createNewXmlDocument("missingFields");

            boolean headersUpdated = processMissingElements(sourceHeaders, targetHeaders, sourceDoc, targetDoc, missingHeadersDoc, "header", "name", XmlUtil.findOrCreateParent(targetDoc, "table-header-map"));
            if(headersUpdated) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
                XmlUtil.saveXmlDocument(missingHeadersDoc, missingHeadersPath);
            }
            boolean foreignTablesUpdated = processMissingElements(sourceForeignTables, targetForeignTables, sourceDoc, targetDoc, missingForeignTablesDoc, "foreign-table", "name", XmlUtil.findOrCreateParent(targetDoc, "foreign-tables"));
            if (foreignTablesUpdated) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
                XmlUtil.saveXmlDocument(missingForeignTablesDoc, missingForeignTablesPath);
            }
            boolean fieldsUpdated = processMissingElements(sourceFields, targetDbFields, sourceDoc, targetDoc, missingFieldsDoc, "field", "db-field", targetDoc.getDocumentElement());
            if (fieldsUpdated) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
                XmlUtil.saveXmlDocument(missingFieldsDoc, missingFieldsPath);
            }
            
            System.out.println("✅ Processing completed. Missing elements saved in both target XML and separate files.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    private boolean processMissingElements(NodeList sourceElements, Set<String> targetElements, Document sourceDoc, Document targetDoc,
    Document missingDoc, String elementTag, String attribute, Element targetParent) {
        
        boolean changesMade = false;
        Element missingRoot = missingDoc.getDocumentElement();

        //store the last order-by value for each section
        Map<String, Integer> sectionOrderMap = new HashMap<>();
        
        System.out.println("Initial values:");
        System.out.println("elementTag: " + elementTag);
        System.out.println("attribute: " + attribute);
        System.out.println("targetElements: " + targetElements);

        for (int i = 0; i < sourceElements.getLength(); i++) {
            Element element = (Element) sourceElements.item(i);
            System.out.println("\nProcessing element " + (i + 1) + "/" + sourceElements.getLength());
            System.out.println("Element: " + XmlUtil.nodeToString(element));

            String elementValue = XmlUtil.getElementAttributeOrText(element, attribute);
            // System.out.println("Extracted elementValue: " + elementValue);

            if (attribute.equals("db-field")) {
                elementValue = XmlUtil.extractDbFieldValue(element).trim().toUpperCase();
                System.out.println("Transformed db-field elementValue: " + elementValue);
            }

            if (!targetElements.contains(elementValue)) {
                System.out.println("❗ Element missing in targetElements: " + elementValue);

                // Extract header-name and section number for db-field attributes
                if (attribute.equals("db-field")) {
                    //element is from sourceDoc
                    String sectionValue = XmlUtil.getSection(element);
                    System.out.println("Extracted sectionValue from SourceEle: " + sectionValue);

                    // First get headerName from sourceDoc
                    String headerName = XmlUtil.getHeaderNameBySection(sourceDoc, sectionValue);
                    System.out.println("Extracted headerName: " + headerName);

                    // Find existing section in target XML
                    Element existingHeader = XmlUtil.findHeaderByName(targetDoc, headerName);
                    System.out.println("Existing header in targetDoc: " + (existingHeader != null ? XmlUtil.nodeToString(existingHeader) : "null"));

                    if (existingHeader != null) {
                        // Fetch the correct section number from target XML
                        String targetSectionValue = XmlUtil.getSection(existingHeader);

                        int nextOrderBy = sectionOrderMap.compute(targetSectionValue, (sec, currentVal) -> {
                            if (currentVal == null) {
                                return XmlUtil.getLastOrderBy(targetDoc, sec) + 1;
                            } else {
                                return currentVal + 1;
                            }
                        });
                        System.out.println("Next order-by value for section " + targetSectionValue + ": " + nextOrderBy);

                        // Update section
                        if(!targetSectionValue.equals(sectionValue)){
                            //get the section Node from the element
                            NodeList sectionNode = element.getElementsByTagName("section");
                            if (sectionNode.getLength() > 0) {
                                Element sectionElement = (Element) sectionNode.item(0);
                                sectionElement.setTextContent(String.valueOf(targetSectionValue));
                                System.out.println("✅ <section> element updated to: " + targetSectionValue);
                            }
                        }
                        // Update order-by
                        NodeList orderByNode = element.getElementsByTagName("order-by");
                        if (orderByNode.getLength() > 0) {
                            Element orderByElement = (Element) orderByNode.item(0);
                            orderByElement.setTextContent(String.valueOf(nextOrderBy));
                            System.out.println("✅ <order-by> element updated to: " + nextOrderBy);
                        }
                        System.out.println(XmlUtil.elementToString(element));
                    } else {
                        System.out.println("⚠️ Warning: No header found for name " + headerName);
                    }
                }

                // Append to correct parent in target XML
                Node importedNodeTarget = targetDoc.importNode(element, true);
                targetParent.appendChild(importedNodeTarget);
                System.out.println("✅ Added element to target XML: " + XmlUtil.nodeToString(importedNodeTarget));

                // Append to missing elements XML
                Node importedNodeMissing = missingDoc.importNode(element, true);
                missingRoot.appendChild(importedNodeMissing);
                System.out.println("✅ Added element to missing XML: " + XmlUtil.nodeToString(importedNodeMissing));

                changesMade = true;
            }
        }
        System.out.println("Changes made: " + changesMade);
        return changesMade;
    }

}
 
 