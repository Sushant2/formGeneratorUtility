package com.utility.xmlUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class XmlService {
    public void processXmlFiles(String sourcePath, String targetPath) {

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

            boolean headersUpdated = processMissingElements(sourceHeaders, targetHeaders, sourceDoc, targetDoc, "header", "name", XmlUtil.findOrCreateParent(targetDoc, "table-header-map"));
            if (headersUpdated) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
            }
            boolean foreignTablesUpdated = processMissingElements(sourceForeignTables, targetForeignTables, sourceDoc,
                    targetDoc, "foreign-table", "name", XmlUtil.findOrCreateParent(targetDoc, "foreign-tables"));
            if (foreignTablesUpdated) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
            }
            boolean fieldsUpdated = processMissingElements(sourceFields, targetDbFields, sourceDoc, targetDoc, "field", "db-field", targetDoc.getDocumentElement());
            if (fieldsUpdated) {
                // fix order-by values in target XML
                XmlUtil.fixOrderByPerSection(targetDoc);
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
            }

            System.out.println("Processing completed. Missing elements saved in target XML.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean processMissingElements(NodeList sourceElements, Set<String> targetElements, Document sourceDoc, Document targetDoc, String elementTag, String attribute, Element targetParent) {

        boolean changesMade = false;
        Map<String, Integer> sectionOrderMap = new HashMap<>();

        for (int i = 0; i < sourceElements.getLength(); i++) {
            Element sourceField = (Element) sourceElements.item(i);

            String elementValue = XmlUtil.getElementAttributeOrText(sourceField, attribute);
            if (attribute.equals("db-field")) {
                elementValue = XmlUtil.extractDbFieldValue(sourceField).trim().toUpperCase();
            }

            if (!targetElements.contains(elementValue)) {
                System.out.println("Missing field found: " + elementValue);

                if (attribute.equals("db-field")) {

                    String sectionValue = XmlUtil.getSection(sourceField);
                    String headerName = XmlUtil.getHeaderNameBySection(sourceDoc, sectionValue);
                    Element headerInTarget = XmlUtil.findHeaderByName(targetDoc, headerName);

                    if (headerInTarget == null) {
                        System.out.println("Skipping: Header not found in target for section: " + sectionValue);
                        continue;
                    }

                    // Get correct section from target
                    String targetSectionValue = XmlUtil.getSection(headerInTarget);

                    // Update section
                    if(!targetSectionValue.equals(sectionValue)){
                        //get the section Node from the element
                        NodeList sectionNode = sourceField.getElementsByTagName("section");
                        if (sectionNode.getLength() > 0) {
                            Element sectionElement = (Element) sectionNode.item(0);
                            sectionElement.setTextContent(String.valueOf(targetSectionValue));
                            System.out.println("<section> element updated to: " + targetSectionValue);
                        }
                    }

                    // Compute next order-by for this section
                    int nextOrderBy = sectionOrderMap.compute(targetSectionValue, (sec, curr) -> {
                        return (curr == null) ? XmlUtil.getLastOrderBy(targetDoc, sec) + 1 : curr + 1;
                    });

                    // Use template based on display-type
                    String displayType = XmlUtil.getDisplayType(sourceField);
                    boolean isMultiSelect = XmlUtil.isMultiSelect(sourceField);

                    Element template = XmlNodeTemplate.getTemplateByType(displayType, isMultiSelect);
                    if (template == null) {
                        System.out.println("Skipping: No template found for display-type: " + displayType);
                        continue;
                    }

                    // Clone and modify template
                    Element newField = updateXMLNode(sourceField, displayType, isMultiSelect, nextOrderBy, targetDoc, template, targetParent);

                    // Add to target XML
                    if (newField != null) {
                        targetParent.appendChild(targetDoc.importNode(newField, true));
                        System.out.println("Added missing field to target: " + elementValue);
                        changesMade = true;
                    }else{
                        System.out.println("Failed to create new field from template.");
                    }
                }else{
                    // For other elements(sections, headers), just import them directly
                    targetParent.appendChild(targetDoc.importNode(sourceField, true));
                    changesMade = true;
                    System.out.println("Added missing element to target: " + elementValue);
                }
            }
        }

        return changesMade;
    }

    public static Element updateXMLNode(Element sourceField, String displayType,boolean isMultiSelect, int nextOrderBy,
            Document targetDoc, Element template, Element targetParent) { 
        try {

            if (template != null) {

                // No need to deep clone the template again
                // Element clonedTemplate = template;

                Element clonedTemplate = (Element) targetDoc.importNode(template, true);

                // Replace key attributes
                XmlUtil.replaceChildValue(clonedTemplate, "field-name", XmlUtil.getValue(sourceField, "field-name"));
                XmlUtil.replaceChildValue(clonedTemplate, "display-name", XmlUtil.getValue(sourceField, "display-name"));
                XmlUtil.replaceChildValue(clonedTemplate, "db-field", XmlUtil.getValue(sourceField, "db-field"));
                XmlUtil.replaceChildValue(clonedTemplate, "data-type", XmlUtil.getValue(sourceField, "data-type"));
                XmlUtil.replaceChildValue(clonedTemplate, "section", XmlUtil.getValue(sourceField, "section"));
                XmlUtil.replaceChildValue(clonedTemplate, "order-by", String.valueOf(nextOrderBy));

                // Special handling for Combo display-type
                if (isMultiSelect) {
                    XmlUtil.replaceOrInsertChild(clonedTemplate, "is-multiselect", "true");
                }

                // If sourcField has mail merge node, import it to clonedTemplate
                Node mailMergeNode = XmlUtil.getDirectChildNode(sourceField, "mailmerge");
                if (mailMergeNode != null) {
                    Node importedMailMerge = targetDoc.importNode(mailMergeNode, true);
                    clonedTemplate.appendChild(importedMailMerge);
                }

                System.out.println("Field modified from template: " + XmlUtil.getValue(sourceField, "db-field"));

                return clonedTemplate; // Return newly created Element
            } else {
                System.out.println("No template found for display-type: " + displayType);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error updating XML node from template: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
