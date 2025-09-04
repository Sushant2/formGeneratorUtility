package com.utility.xmlUtility;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
public class XmlService {
    public static HashMap<String, String> updatedHeaders = new HashMap<>();
    public void processXmlFiles(String sourcePath, String targetPath, Set<String> underscoreFieldsSet) {

        try {
            Document sourceDoc = null;
            Document targetDoc = null;
            
            sourceDoc = XmlUtil.loadXmlDocument(sourcePath);

            if(targetPath != null && Files.exists(Paths.get(targetPath))){
                targetDoc = XmlUtil.loadXmlDocument(targetPath);
            }else{

                // Base directory where target files will be created
                String baseDir = "src/main/resources/tabModulesXml";  // Define base directory for target path
                // Ensure the targetPath is relative to the base directory
                if (targetPath != null && targetPath.startsWith("/tabModulesXml")) {
                    targetPath = baseDir + targetPath.substring("/tabModulesXml".length());
                }

                // Check if the target path exists
                File targetFile = new File(targetPath);
                File parentDir = targetFile.getParentFile();

                if (parentDir != null && !parentDir.exists()) {
                    // Create missing parent directories
                    boolean dirsCreated = parentDir.mkdirs();
                    if (dirsCreated) {
                        System.out.println("Created missing folders for: " + targetPath);
                    } else {
                        System.out.println("Failed to create directories for: " + targetPath);
                    }
                }

                // Create a new empty XML with <table> root
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                targetDoc = docBuilder.newDocument();
                Element rootElement = targetDoc.createElement("table");
                targetDoc.appendChild(rootElement);

                System.out.println("Target XML not found. Created new empty XML for: " + targetPath);
                
                // Copy the content between <table> and <header> from the source to the target XML
                XmlUtil.copyTableContent(sourceDoc, targetDoc);
                XmlUtil.saveXmlDocument(targetDoc, targetPath); // Save immediately after creating empty
            }

            // Extract existing elements from target XML
            Set<String> targetHeaders = XmlUtil.extractElements(targetDoc, "header", "name");
            Set<String> targetForeignTables = XmlUtil.extractElements(targetDoc, "foreign-table", "name");
            Set<String> targetDbFields = XmlUtil.extractElements(targetDoc, "db-field", "name");
            Set<String> targetHeadersValue = XmlUtil.extractElements(targetDoc, "header", "value");

            NodeList sourceHeaders = sourceDoc.getElementsByTagName("header");
            NodeList sourceForeignTables = sourceDoc.getElementsByTagName("foreign-table");
            NodeList sourceFields = sourceDoc.getElementsByTagName("field");

            boolean headersUpdated = processMissingElements(sourceHeaders, targetHeaders, targetHeadersValue, sourceDoc, targetDoc, "header", "name", XmlUtil.findOrCreateParent(targetDoc, "table-header-map"), underscoreFieldsSet, sourcePath);
            if (headersUpdated) {
                XmlUtil.setHeaderOrders(targetDoc);
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
            }
            boolean foreignTablesUpdated = processMissingElements(sourceForeignTables, targetForeignTables, null, sourceDoc,
                    targetDoc, "foreign-table", "name", XmlUtil.findOrCreateParent(targetDoc, "foreign-tables"), underscoreFieldsSet, sourcePath);
            if (foreignTablesUpdated) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
            }
            boolean fieldsUpdated = processMissingElements(sourceFields, targetDbFields, null, sourceDoc, targetDoc, "field", "db-field", targetDoc.getDocumentElement(), underscoreFieldsSet, sourcePath);
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

    private boolean processMissingElements(NodeList sourceElements, Set<String> targetElements, Set<String> targetHeaderValues, Document sourceDoc, Document targetDoc, String elementTag, String attribute, Element targetParent, Set<String> underscoreFieldsSet, String sourcePath) {

        boolean changesMade = false;
        Map<String, Integer> sectionOrderMap = new HashMap<>();
        Map<String, Element> targetFieldMap = XmlUtil.buildTargetFieldMap(targetDoc);
        
        for (int i = 0; i < sourceElements.getLength(); i++) {
            Element sourceField = (Element) sourceElements.item(i);
            Element clonedSourceField = (Element) sourceField.cloneNode(true);

            String elementValue = XmlUtil.getElementAttributeOrText(clonedSourceField, attribute);
            if (attribute.equals("db-field")) {
                elementValue = XmlUtil.extractDbFieldValue(clonedSourceField).trim().toUpperCase();
            }

            if (!targetElements.contains(elementValue)) {
                System.out.println("Missing field found: " + elementValue);

                if (attribute.equals("db-field")) {

                    // skip if <field-name>bestTimeToContact</field-name> is already in target
                    if(sourcePath != null && (sourcePath.contains("fsLeadDetails.xml") || sourcePath.contains("fsLeadDetails_copy.xml"))){
                        boolean isBestTimeToContact = XmlUtil.getValue(clonedSourceField, "field-name").equals("bestTimeToContact");
                        if(isBestTimeToContact){
                            System.out.println("Skipping: bestTimeToContact field is already in target");
                            continue;
                        }
                    }

                    // skip if <field-name>typeOfOwnership</field-name> is already in target
                    if(sourcePath != null && (sourcePath.contains("fimOwners.xml") || sourcePath.contains("fimOwners_copy.xml"))){
                        boolean isTypeOfOwnership = XmlUtil.getValue(clonedSourceField, "field-name").equals("typeOfOwnership");
                        if(isTypeOfOwnership){
                            System.out.println("Skipping: typeOfOwnership field is already in target");
                            continue;
                        }
                    }

                    boolean isAreaManager = XmlUtil.getValue(clonedSourceField, "field-name").equals("areaManager");

                    //handle preferredStateId3, preferredStateId3
                    if(elementValue.equals("PREFERRED_COUNTRY3")){
                        Node fieldNode = getPreferredFields("PREFERRED_COUNTRY3");
                        Node adopted = targetDoc.importNode(fieldNode, true);
                        targetDoc.getDocumentElement().appendChild(adopted);
                        continue;
                    }else if(elementValue.equals("PREFERRED_STATE_ID3")){
                        Node fieldNode = getPreferredFields("PREFERRED_STATE_ID3");
                        Node adopted = targetDoc.importNode(fieldNode, true);
                        targetDoc.getDocumentElement().appendChild(adopted);
                        continue;
                    }

                    String sectionValue = XmlUtil.getSection(clonedSourceField);
                    String fieldName = XmlUtil.getValue(clonedSourceField, "field-name");
                    
                    // Special handling for system fields without sections (idField and entityID)
                    boolean isSystemField = sectionValue.isEmpty() && 
                                          (fieldName.equals("idField") || fieldName.equals("entityID"));
                    
                    if (isSystemField) {
                        System.out.println("Adding system field without section: " + elementValue + " (field-name: " + fieldName + ")");
                        targetParent.appendChild(targetDoc.importNode(clonedSourceField, true));
                        changesMade = true;
                        continue;
                    }
                    
                    String headerName = XmlUtil.getHeaderNameBySection(sourceDoc, sectionValue);
                    Element headerInTarget = null;
                    if(updatedHeaders.containsKey(headerName))
                        headerName = updatedHeaders.get(headerName);

                    headerInTarget = XmlUtil.findHeaderByName(targetDoc, headerName);

                    if (headerInTarget == null) {
                        // Handling if "tabModules" : targetElements is empty
                        if(targetElements.isEmpty()){
                            targetParent.appendChild(targetDoc.importNode(clonedSourceField, true));
                            changesMade = true;
                            System.out.println("Added missing element to target: " + elementValue);
                            continue;
                        }else{
                            System.out.println("Skipping: Header not found in target for section: " + sectionValue + " (field-name: " + fieldName + ")");
                            continue;
                        }
                    }

                    // Get correct section from target
                    String targetSectionValue = XmlUtil.getSection(headerInTarget);

                    // Update section
                    if(!targetSectionValue.equals(sectionValue)){
                        //get the section Node from the element
                        NodeList sectionNode = clonedSourceField.getElementsByTagName("section");
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

                    // Use template based on display-type, is-multiselect
                    String displayType = XmlUtil.getDisplayType(clonedSourceField);
                    if(displayType.equals("Label")){
                        String dataType = XmlUtil.getValue(clonedSourceField, "data-type");
                        if(dataType.equals("Date")){
                            displayType = "Date";
                        }else if(dataType.equals("String")){
                            displayType = "Text";
                        }
                    }
                    boolean isMultiSelect = XmlUtil.isMultiSelect(clonedSourceField);
                    if(isAreaManager){
                        isMultiSelect = true;
                    }

                    Element template = XmlNodeTemplate.getTemplateByType(displayType, isMultiSelect);
                    if (template == null) {
                        System.out.println("Skipping: No template found for display-type: " + displayType);
                        continue;
                    }

                    // Clone and modify template
                    Element newField = updateXMLNode(clonedSourceField, displayType, isMultiSelect, nextOrderBy, targetDoc, template, targetParent, underscoreFieldsSet);
                    if(isAreaManager){
                        newField.appendChild(XmlUtil.createElement(targetDoc, "dropdown-option", "4"));
                        
                        // Create combo element with proper nested structure
                        Element combo = targetDoc.createElement("combo");
                        combo.appendChild(XmlUtil.createElement(targetDoc, "combo-source-values-method", "fetchUserDataBasedOnRoleIds"));
                        newField.appendChild(combo);
                        
                        newField.appendChild(XmlUtil.createElement(targetDoc, "transform-method", "fetchUserNameBasedOnUserIds"));
                        XmlUtil.replaceChildValue(newField, "data-type", "String");
                    }

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

                    //Special handling for header
                    if ("header".equals(elementTag)) {
                        String sourceHeaderValue = clonedSourceField.getAttribute("value");
                        if(targetHeaderValues != null && targetHeaderValues.contains(sourceHeaderValue)){
                            System.out.println("Skipping: Header already exists in target with value: " + sourceHeaderValue);
                            continue;
                        }
                        //if headerName not starts with "bSec_" then append bSec_ in front of headerName
                        if(!elementValue.startsWith("bSec_")){
                            String prevValue = elementValue;
                            elementValue = "bSec_" + elementValue;
                            //generate random number in between 1000000000 and 9999999999
                            int randomNumber = (int)(Math.random() * 9000000) + 1000000;
                            elementValue = elementValue + randomNumber;
                            updatedHeaders.put(prevValue, elementValue);

                            //now update the CLONED field with new headerName
                            clonedSourceField.setAttribute("name", elementValue);
                            // Update the section element value with the header name (elementValue)
                            NodeList sectionNodes = clonedSourceField.getElementsByTagName("section");
                            if (sectionNodes.getLength() > 0) {
                                Element sectionElement = (Element) sectionNodes.item(0);
                                sectionElement.setTextContent(elementValue);
                                System.out.println("Updated section element value to: " + elementValue);
                            }
                        }
                        if(sourcePath != null && (sourcePath.contains("franchisees.xml") || sourcePath.contains("franchisees_copy.xml"))){
                            // Check if store timings header already exists before adding it
                            Element existingStoreTimingsHeader = XmlUtil.findHeaderByName(targetDoc, "bSec_storetimings1282868853");
                            if(existingStoreTimingsHeader == null){
                                //adding this header to the target
                                XmlUtil.addStoreTimingsHeader(targetDoc, targetParent);
                                changesMade = true;
                                System.out.println("Added store timings header to target: " + elementValue);
                            } else {
                                System.out.println("Store timings header already exists in target, skipping addition");
                            }
                        }
                    }
                    //Special handling for foreign-table
                    if("foreign-table".equals(elementTag)){
                        if(sourcePath != null && (sourcePath.contains("franchisees.xml") || sourcePath.contains("franchisees_copy.xml"))){
                            // check if below foreign table already exists before adding it
                            Element existingStoreTimingsForeignTable = XmlUtil.findForeignTableByName(targetDoc, "storehoursnd21214306162");
                            if(existingStoreTimingsForeignTable == null){
                                //adding this foreign table to the target
                                XmlUtil.addStoreTimingsForeignTable(targetDoc, targetParent);
                                changesMade = true;
                                System.out.println("Added store timings foreign table to target: " + elementValue);
                            } else {
                                System.out.println("Store timings foreign table already exists in target, skipping addition");
                            }
                        }
                    }
                    targetParent.appendChild(targetDoc.importNode(clonedSourceField, true));
                    changesMade = true;
                    System.out.println("Added missing element to target: " + elementValue);
                }
            }else{
                //if targetElements contains elementValue, then update the tags if mismatch
                if (attribute.equals("db-field")) {
                    // db-field exists, so just update display-name if mismatch
                    XmlUtil.updateTagsIfDiff(clonedSourceField, targetFieldMap);
                    changesMade = true;
                }
            }
        }

        return changesMade;
    }

    public static Element updateXMLNode(Element sourceField, String displayType,boolean isMultiSelect, int nextOrderBy,
            Document targetDoc, Element template, Element targetParent, Set<String> underscoreFieldsSet) { 
        try {

            if (template != null) {

                // No need to deep clone the template again
                // Element clonedTemplate = template;

                Element clonedTemplate = (Element) targetDoc.importNode(template, true);

                // Replace key attributes
                String fieldName = XmlUtil.getValue(sourceField, "field-name");
                if(!fieldName.startsWith("_")) 
                    underscoreFieldsSet.add(fieldName);
                XmlUtil.replaceChildValue(clonedTemplate, "field-name", fieldName.startsWith("_") ? fieldName : "_" + fieldName);
                XmlUtil.replaceChildValue(clonedTemplate, "display-name", XmlUtil.getValue(sourceField, "display-name"));
                XmlUtil.replaceChildValue(clonedTemplate, "db-field", XmlUtil.getValue(sourceField, "db-field"));
                XmlUtil.replaceChildValue(clonedTemplate, "data-type", XmlUtil.getValue(sourceField, "data-type"));
                XmlUtil.replaceChildValue(clonedTemplate, "section", XmlUtil.getValue(sourceField, "section"));
                XmlUtil.replaceChildValue(clonedTemplate, "is-active", XmlUtil.getValue(sourceField, "is-active"));
                XmlUtil.replaceChildValue(clonedTemplate, "is-mandatory", XmlUtil.getValue(sourceField, "is-mandatory"));
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

                // If sourceField has sync node, import it to clonedTemplate
                Node syncNode = XmlUtil.getDirectChildNode(sourceField, "sync");
                Node syncWithNode = XmlUtil.getDirectChildNode(sourceField, "sync-with");
                if (syncNode != null) {
                    Node importedSync = targetDoc.importNode(syncNode, true);
                    clonedTemplate.appendChild(importedSync);
                    System.out.println("Added sync tag for field: " + XmlUtil.getValue(sourceField, "db-field"));
                }else if (syncWithNode != null) {
                    Node importedSyncWith = targetDoc.importNode(syncWithNode, true);
                    clonedTemplate.appendChild(importedSyncWith);
                    System.out.println("Added sync-with tag for field: " + XmlUtil.getValue(sourceField, "db-field"));
                }

                // If sourceField has dependent node, import it to clonedTemplate
                Node dependentNode = XmlUtil.getDirectChildNode(sourceField, "dependent");
                Node dependentParentNode = XmlUtil.getDirectChildNode(sourceField, "dependent-parent");
                if(dependentNode != null){
                    Node importedDependent = targetDoc.importNode(dependentNode, true);
                    clonedTemplate.appendChild(importedDependent);
                    System.out.println("Added dependent tag for field: " + XmlUtil.getValue(sourceField, "db-field"));
                }else if(dependentParentNode != null){
                    Node importedDependentParent = targetDoc.importNode(dependentParentNode, true);
                    clonedTemplate.appendChild(importedDependentParent);
                    System.out.println("Added dependent-parent tag for field: " + XmlUtil.getValue(sourceField, "db-field"));
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

    public Node getPreferredFields(String elementValue){
        if(elementValue.equals("PREFERRED_COUNTRY3")){
            try {
                // Create a new document to build the field node
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();
                
                // Create the field element
                Element field = doc.createElement("field");
                field.setAttribute("summary", "true");
                
                // Add all child elements
                field.appendChild(XmlUtil.createElement(doc, "field-name", "_preferredCountry3"));
                field.appendChild(XmlUtil.createElement(doc, "display-name", "Preferred Country 3"));
                field.appendChild(XmlUtil.createElement(doc, "db-field", "PREFERRED_COUNTRY3"));
                field.appendChild(XmlUtil.createElement(doc, "data-type", "String"));
                field.appendChild(XmlUtil.createElement(doc, "display-type", "Combo"));
                field.appendChild(XmlUtil.createElement(doc, "group-by", "true"));
                field.appendChild(XmlUtil.createElement(doc, "section", "3"));
                field.appendChild(XmlUtil.createElement(doc, "is-active", "yes"));
                field.appendChild(XmlUtil.createElement(doc, "is-mandatory", "false"));
                field.appendChild(XmlUtil.createElement(doc, "build-field", "no"));
                field.appendChild(XmlUtil.createElement(doc, "field-export", "true"));
                field.appendChild(XmlUtil.createElement(doc, "order-by", "6"));
                field.appendChild(XmlUtil.createElement(doc, "dropdown-option", "2"));
                
                // Create combo element
                Element combo = doc.createElement("combo");
                combo.appendChild(XmlUtil.createElement(doc, "parent", "true"));
                combo.appendChild(XmlUtil.createElement(doc, "dependent-field", "_preferredStateId3"));
                combo.appendChild(XmlUtil.createElement(doc, "combo-source-values-method", "comboFimCountry"));
                field.appendChild(combo);
                
                field.appendChild(XmlUtil.createElement(doc, "transform-method", "transformCountryFromId"));
                field.appendChild(XmlUtil.createElement(doc, "src-table", "countries"));
                field.appendChild(XmlUtil.createElement(doc, "src-field", "countryID"));
                field.appendChild(XmlUtil.createElement(doc, "src-value", "name"));
                
                // Create mailmerge element
                Element mailmerge = doc.createElement("mailmerge");
                mailmerge.setAttribute("is-active", "true");
                mailmerge.setAttribute("keyword-name", "$fsLeadDet_preferredCountry3$");
                field.appendChild(mailmerge);
                
                field.appendChild(XmlUtil.createElement(doc, "pii-enabled", "false"));
                
                return field;
                
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        } else if (elementValue.equals("PREFERRED_STATE_ID3")) {
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.newDocument();
        
                Element field = doc.createElement("field");
                field.setAttribute("summary", "true");
        
                field.appendChild(XmlUtil.createElement(doc, "field-name", "_preferredStateId3"));
                field.appendChild(XmlUtil.createElement(doc, "display-name", "Preferred State / Province 3"));
                field.appendChild(XmlUtil.createElement(doc, "db-field", "PREFERRED_STATE_ID3"));
                field.appendChild(XmlUtil.createElement(doc, "group-by", "true"));
                field.appendChild(XmlUtil.createElement(doc, "data-type", "String"));
                field.appendChild(XmlUtil.createElement(doc, "display-type", "Combo"));
                field.appendChild(XmlUtil.createElement(doc, "section", "3"));
                field.appendChild(XmlUtil.createElement(doc, "is-active", "yes"));
                field.appendChild(XmlUtil.createElement(doc, "is-mandatory", "false"));
                field.appendChild(XmlUtil.createElement(doc, "build-field", "no"));
                field.appendChild(XmlUtil.createElement(doc, "field-export", "true"));
                field.appendChild(XmlUtil.createElement(doc, "order-by", "8"));
                field.appendChild(XmlUtil.createElement(doc, "dropdown-option", "2"));
        
                Element combo = doc.createElement("combo");
                combo.appendChild(XmlUtil.createElement(doc, "parent", "false"));
                combo.appendChild(XmlUtil.createElement(doc, "dependent-field", "_preferredCountry3"));
                combo.appendChild(XmlUtil.createElement(doc, "combo-source-values-method", "comboFimState"));
                combo.appendChild(XmlUtil.createElement(doc, "combo-method-param", "_preferredCountry3"));
                field.appendChild(combo);
        
                field.appendChild(XmlUtil.createElement(doc, "transform-method", "transformStateFromId"));
                field.appendChild(XmlUtil.createElement(doc, "src-table", "regions"));
                field.appendChild(XmlUtil.createElement(doc, "src-field", "regionNo"));
                field.appendChild(XmlUtil.createElement(doc, "src-value", "regionName"));
        
                Element mailmerge = doc.createElement("mailmerge");
                mailmerge.setAttribute("is-active", "true");
                mailmerge.setAttribute("keyword-name", "$fsLeadDet_preferredStateId3$");
                field.appendChild(mailmerge);
        
                field.appendChild(XmlUtil.createElement(doc, "pii-enabled", "false"));
        
                return field;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        return null;
    }
}
