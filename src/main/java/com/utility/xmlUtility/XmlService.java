package com.utility.xmlUtility;
 
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
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
            boolean foreignTablesUpdated = processMissingElements(sourceForeignTables, targetForeignTables, sourceDoc, targetDoc, missingForeignTablesDoc, "foreign-table", "name", XmlUtil.findOrCreateParent(targetDoc, "foreign-tables"));
            boolean fieldsUpdated = processMissingElements(sourceFields, targetDbFields, sourceDoc, targetDoc, missingFieldsDoc, "field", "db-field", targetDoc.getDocumentElement());

            // Save updated target XML
            if (fieldsUpdated || headersUpdated || foreignTablesUpdated) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
            }
            
            // Save missing elements in separate files
            if (fieldsUpdated) XmlUtil.saveXmlDocument(missingFieldsDoc, missingFieldsPath);
            if (headersUpdated) XmlUtil.saveXmlDocument(missingHeadersDoc, missingHeadersPath);
            if (foreignTablesUpdated) XmlUtil.saveXmlDocument(missingForeignTablesDoc, missingForeignTablesPath);

            System.out.println("✅ Processing completed. Missing elements saved in both target XML and separate files.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    private boolean processMissingElements(NodeList sourceElements, Set<String> targetElements, Document sourceDoc, Document targetDoc,
    Document missingDoc, String elementTag, String attribute, Element targetParent) {
        boolean changesMade = false;

        Element missingRoot = missingDoc.getDocumentElement();

        for (int i = 0; i < sourceElements.getLength(); i++) {
            Element element = (Element) sourceElements.item(i);
            String elementValue = XmlUtil.getElementAttributeOrText(element, attribute);

            if (attribute.equals("db-field")) {
                elementValue = XmlUtil.extractDbFieldValue(element).trim().toUpperCase();
            }

            if (!targetElements.contains(elementValue)) {

                System.out.println("Element value -->" + elementValue);

                // Extract header-name and section number for db-field attributes
                if (attribute.equals("db-field")) {
                    String sectionValue = XmlUtil.getSection(element);
                    //first get headerName from sourceDoc
                    String headerName = XmlUtil.getHeaderNameBySection(targetDoc, sectionValue);
                    if(headerName.isEmpty()){
                        headerName = XmlUtil.getHeaderNameBySection(sourceDoc, sectionValue);
                    }
                    
                    System.out.println("🔹 Extracted headerName: " + headerName + ", sectionValue: " + sectionValue);


                    // Find existing section in target XML
                    Element existingHeader = XmlUtil.findHeaderByName(targetDoc, headerName);

                    if (existingHeader != null) {
                        // Fetch the correct section number from target XML
                        String targetSectionValue = XmlUtil.getSection(existingHeader);
                        System.out.println("✅ Updating section from " + sectionValue + " → " + targetSectionValue);

                        // Update section and order-by
                        element.setAttribute("section", targetSectionValue);
                        int lastOrderBy = XmlUtil.getLastOrderBy(existingHeader);
                        element.setAttribute("order-by", String.valueOf(lastOrderBy + 1));
                    } else {
                        System.out.println("⚠️ Warning: No header found for name " + headerName);
                    }
                }

                // Append to correct parent in target XML
                Node importedNodeTarget = targetDoc.importNode(element, true);
                targetParent.appendChild(importedNodeTarget);

                // Append to missing elements XML
                Node importedNodeMissing = missingDoc.importNode(element, true);
                missingRoot.appendChild(importedNodeMissing);

                System.out.println("Added missing " + elementTag + ": " + elementValue);
                changesMade = true;
            }
        }
        return changesMade;
    }
}
 
 