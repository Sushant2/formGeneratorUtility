package com.utility.xmlUtility;

import org.springframework.stereotype.Service;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Service
public class xmlService {

    public void processXmlFiles(String sourceFile, String targetFile, String missingFieldsFile,
            String missingHeadersFile, String missingForeignTablesFile) {
        try {
            Document sourceDoc = xmlUtil.loadXmlDocument(sourceFile);
            Document targetDoc = xmlUtil.loadXmlDocument(targetFile);
            Document missingFieldsDoc = xmlUtil.createNewXmlDocument("missingFields");

            Set<String> targetFields = xmlUtil.extractElements(targetDoc, "db-field");

            NodeList sourceFields = sourceDoc.getElementsByTagName("field");
            Element targetParent = targetDoc.getDocumentElement();

            boolean changesMade = processMissingElements(sourceFields, targetFields, sourceDoc, targetDoc,
                    missingFieldsDoc, "field", "db-field", targetParent);

            if (changesMade) {
                xmlUtil.saveXmlDocument(targetDoc, targetFile);
                xmlUtil.saveXmlDocument(missingFieldsDoc, missingFieldsFile);
                System.out.println("✅ XML processing complete. Changes saved.");
            } else {
                System.out.println("✅ No missing fields found.");
            }

        } catch (Exception e) {
            System.err.println("⚠️ Error processing XML files: " + e.getMessage());
        }
    }

    private boolean processMissingElements(NodeList sourceElements, Set<String> targetElements, Document sourceDoc, Document targetDoc, Document missingDoc, String elementTag, String attribute, Element targetParent) {
        boolean changesMade = false;

        Element missingRoot = missingDoc.getDocumentElement();

        for (int i = 0; i < sourceElements.getLength(); i++) {
            Element element = (Element) sourceElements.item(i);

            // we'll clonedEle into targetDoc
            Element clonedElement = (Element) element.cloneNode(true); // Clone the element

            String elementValue = xmlUtil.getElementAttributeOrText(element, attribute);

            if (attribute.equals("db-field")) {
                elementValue = xmlUtil.extractDbFieldValue(element).trim().toUpperCase();
            }

            if (!targetElements.contains(elementValue)) {

                System.out.println("Element value -->" + elementValue);

                // Extract header-name and section number for db-field attributes
                if (attribute.equals("db-field")) {
                    String sectionValue = xmlUtil.getSection(clonedElement);
                    // first get headerName from sourceDoc
                    String headerName = xmlUtil.getHeaderNameBySection(targetDoc, sectionValue);
                    if (headerName.isEmpty()) {
                        headerName = xmlUtil.getHeaderNameBySection(sourceDoc, sectionValue);
                    }

                    System.out.println("🔹 Extracted headerName: " + headerName + ", sectionValue: " + sectionValue);

                    // Find existing section in target XML
                    Element existingHeader = xmlUtil.findHeaderByName(targetDoc, headerName);

                    if (existingHeader != null) {
                        // Fetch the correct section number from target XML
                        String targetSectionValue = xmlUtil.getSection(existingHeader);
                        System.out.println("✅ Updating section from " + sectionValue + " → " + targetSectionValue);

                        // Update section and order-by
                        int lastOrderBy = xmlUtil.getLastOrderBy(existingHeader);
                        clonedElement.setAttribute("section", targetSectionValue);
                        clonedElement.setAttribute("order-by", String.valueOf(lastOrderBy + 1));
                    } else {
                        System.out.println("⚠️ Warning: No header found for name " + headerName);
                    }
                }

                Element elementToImport = attribute.equals("db-field") ? clonedElement : element;

                // Append to correct parent in target XML
                Node importedNodeTarget = targetDoc.importNode(elementToImport, true);
                targetParent.appendChild(importedNodeTarget);

                // Append to missing elements XML
                Node importedNodeMissing = missingDoc.importNode(elementToImport, true);
                missingRoot.appendChild(importedNodeMissing);

                System.out.println("Added missing " + elementTag + ": " + elementValue);
                changesMade = true;
            }
        }
        return changesMade;
    }
}
