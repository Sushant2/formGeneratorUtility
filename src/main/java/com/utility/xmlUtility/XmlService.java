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
                } else if (targetPath != null && targetPath.startsWith("/tabularSectionMappingsXml")) {
                    baseDir = "src/main/resources/tabularSectionMappingsXml";
                    targetPath = baseDir + targetPath.substring("/tabularSectionMappingsXml".length());
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

                    // set is-active element with value "no" for legalEntityDetails section
                    if(sourcePath != null && (sourcePath.contains("franchisees.xml") || sourcePath.contains("franchisees_copy.xml"))){
                        // Special handling for legalEntityDetails section - set all fields as inactive
                        String sectionValue = XmlUtil.getSection(clonedSourceField);
                        String headerName = XmlUtil.getHeaderNameBySection(sourceDoc, sectionValue);
                        // Check if this field belongs to legalEntityDetails section by checking if header name contains "legalEntityDetails"
                        if(headerName != null && headerName.toLowerCase().contains("legalentitydetails")){ 
                            XmlUtil.replaceOrInsertChild(clonedSourceField, "is-active", "no");
                            System.out.println("Added is-active element with value 'no' to field in legalEntityDetails section.");
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

                    // Special handling for service11844788279.xml
                    if(sourcePath != null && sourcePath.contains("service11844788279")){
                        // Check if TAB_PRIMARY_ID field already exists before adding it
                        Element existingTabPrimaryIdField = XmlUtil.findFieldByDbField(targetDoc, "TAB_PRIMARY_ID");
                        if(existingTabPrimaryIdField == null){
                            // Add tab primary id field to the target
                            Node tabPrimaryIdFieldNode = getTabPrimaryIdField();
                            if(tabPrimaryIdFieldNode != null){
                                Node adopted = targetDoc.importNode(tabPrimaryIdFieldNode, true);
                                targetDoc.getDocumentElement().appendChild(adopted);
                                changesMade = true;
                                System.out.println("Added tab primary id field to target: TAB_PRIMARY_ID");
                            }
                        } else {
                            System.out.println("Tab primary id field already exists in target, skipping addition");
                        }
                    }

                    // Fields handling for franchisees.xml
                    if(sourcePath != null && (sourcePath.contains("franchisees.xml") || sourcePath.contains("franchisees_copy.xml"))){
                        // Check if franchise fee field already exists before adding it
                        Element existingFranchiseFeeField = XmlUtil.findFieldByDbField(targetDoc, "_FRANCHISE_FEE_1181443611");
                        if(existingFranchiseFeeField == null){
                            // Add franchise fee field to the target
                            Node franchiseFeeFieldNode = getFranchiseFeeField();
                            if(franchiseFeeFieldNode != null){
                                Node adopted = targetDoc.importNode(franchiseFeeFieldNode, true);
                                targetDoc.getDocumentElement().appendChild(adopted);
                                changesMade = true;
                                System.out.println("Added franchise fee field to target: _FRANCHISE_FEE_1181443611");
                            }
                        } else {
                            System.out.println("Franchise fee field already exists in target, skipping addition");
                        }
                        // Special handling for AF_ID field - change display name and set as inactive
                        if(elementValue.equals("AF_ID")){
                            // Change display name to "Area Franchise ID Old"
                            NodeList displayNameNodes = clonedSourceField.getElementsByTagName("display-name");
                            if (displayNameNodes.getLength() > 0) {
                                Element displayNameElement = (Element) displayNameNodes.item(0);
                                displayNameElement.setTextContent("Area Franchise ID Old");
                                System.out.println("Updated AF_ID display name to: Area Franchise ID Old");
                            }
                            
                            // Add is-active element with value "no"
                            Element isActiveElement = sourceDoc.createElement("is-active");
                            isActiveElement.setTextContent("no");
                            clonedSourceField.appendChild(isActiveElement);
                            System.out.println("Added is-active element with value 'no' to AF_ID field");
                        }
                        // Special handling for Supervisor field - set as inactive
                        if(elementValue.equals("SUPERVISOR")){
                            // Add is-active element with value "no"
                            Element isActiveElement = sourceDoc.createElement("is-active");
                            isActiveElement.setTextContent("no");
                            clonedSourceField.appendChild(isActiveElement);
                            System.out.println("Added is-active element with value 'no' to SUPERVISOR field");
                            // Change display name to "Area Franchise ID Old"
                            NodeList displayNameNodes = clonedSourceField.getElementsByTagName("display-name");
                            if (displayNameNodes.getLength() > 0) {
                                Element displayNameElement = (Element) displayNameNodes.item(0);
                                displayNameElement.setTextContent("Supervisor Old");
                                System.out.println("Updated SUPERVISOR display name to: Supervisor Old");
                            }
                        }
                        // Special handling for REGION_ID field - set as not mandatory
                        if(elementValue.equals("REGION_ID")){
                            XmlUtil.replaceOrInsertChild(clonedSourceField, "is-mandatory", "false");
                            System.out.println("Updated is-mandatory element with value 'false' to REGION_ID field");
                        }
                        // Special handling for territoryId field - set as not mandatory
                        if(elementValue.equals("TERRITORY_ID")){
                            XmlUtil.replaceOrInsertChild(clonedSourceField, "is-mandatory", "false");
                            System.out.println("Updated is-mandatory element with value 'false' to TERRITORY_ID field");
                        }
                        // Special handling for FIM_CB_CURRENT_STATUS - set as mandatory
                        if(elementValue.equals("FIM_CB_CURRENT_STATUS")){
                            XmlUtil.replaceOrInsertChild(clonedSourceField, "is-mandatory", "true");
                            System.out.println("Set is-mandatory element with value 'true' to FIM_CB_CURRENT_STATUS field");
                        }
                        // Special handling for RE_OPENING_DATE & STORE_RE_OPENING_DATE as inactive
                        if(elementValue.equals("RE_OPENING_DATE") || elementValue.equals("STORE_RE_OPENING_DATE")){
                            XmlUtil.replaceOrInsertChild(clonedSourceField, "is-active", "no");
                            System.out.println("Set is-active element with value 'no' to RE_OPENING_DATE & STORE_RE_OPENING_DATE fields");
                        }
                        // Special handling for STORE_OPENING_DATE as inactive
                        if(elementValue.equals("STORE_OPENING_DATE")){
                            XmlUtil.replaceOrInsertChild(clonedSourceField, "is-active", "no");
                            System.out.println("Set is-active element with value 'no' to STORE_OPENING_DATE field");
                        }
                        // Special handling for OPENING_DATE as active
                        if(elementValue.equals("OPENING_DATE")){
                            XmlUtil.replaceOrInsertChild(clonedSourceField, "is-active", "yes");
                            System.out.println("Set is-active element with value 'yes' to OPENING_DATE field");
                        }
                    }

                    if(sourcePath != null && (sourcePath.contains("testTabqqqqq1117944734.xml") || sourcePath.contains("testTabqqqqq1117944734_copy.xml"))){
                        Node idFieldNode = clonedSourceField.getElementsByTagName("id-field").item(0);
                        if(idFieldNode != null){
                            idFieldNode.setTextContent("centerTermsID");
                            System.out.println("Updated id-field element to: centerTermsID");
                        }
                        
                        // Check if CENTER_TERMS_ID field already exists in target before adding
                        Element existingCenterTermsIdField = XmlUtil.findFieldByDbField(targetDoc, "CENTER_TERMS_ID");
                        if(existingCenterTermsIdField == null){
                            Node idFieldNewNode = getIdField();
                            if(idFieldNewNode != null){
                                Node adopted = targetDoc.importNode(idFieldNewNode, true);
                                targetDoc.getDocumentElement().appendChild(adopted);
                                changesMade = true;
                                System.out.println("Added centerTermsID field element to target: CENTER_TERMS_ID");
                            }
                        } else {
                            System.out.println("centerTermsID field already exists in target, skipping addition");
                        }
                        
                        // Skip processing if this is the CENTER_TERMS_ID field to avoid duplicates
                        if(elementValue.equals("CENTER_TERMS_ID")){
                            System.out.println("Skipping CENTER_TERMS_ID field processing to avoid duplicates");
                            continue;
                        }
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

                    // If header not found by name, try to find by value (same value but different name)
                    if (headerInTarget == null) {
                        Element sourceHeader = XmlUtil.findHeaderByName(sourceDoc, headerName);
                        if (sourceHeader != null && sourceHeader.hasAttribute("value")) {
                            String sourceHeaderValue = sourceHeader.getAttribute("value");
                            headerInTarget = XmlUtil.findHeaderByValue(targetDoc, sourceHeaderValue);
                            if (headerInTarget != null) {
                                System.out.println("Found header by value (different name but same value): " + sourceHeaderValue + " (field-name: " + fieldName + ")");
                            }
                        }
                    }

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
                            System.out.println("Header already exists in target with value: " + sourceHeaderValue);
                            
                            // Check and copy document elements from source to target header
                            NodeList sourceDocuments = clonedSourceField.getElementsByTagName("documents");
                            if (sourceDocuments.getLength() > 0) {
                                Element sourceDocumentsElement = (Element) sourceDocuments.item(0);
                                
                                // Find the corresponding header in target
                                NodeList targetHeaders = targetDoc.getElementsByTagName("header");
                                for (int j = 0; j < targetHeaders.getLength(); j++) {
                                    Element targetHeader = (Element) targetHeaders.item(j);
                                    if (sourceHeaderValue.equals(targetHeader.getAttribute("value"))) {
                                        // Check if target header has documents element
                                        NodeList targetDocumentsList = targetHeader.getElementsByTagName("documents");
                                        
                                        if (targetDocumentsList.getLength() == 0) {
                                            // Target header has no documents, copy from source
                                            Node importedDocuments = targetDoc.importNode(sourceDocumentsElement, true);
                                            targetHeader.appendChild(importedDocuments);
                                            changesMade = true;
                                            System.out.println("Added documents element to existing header: " + sourceHeaderValue);
                                        } else {
                                            // Target header has documents, check if we need to add missing document children
                                            Element targetDocumentsElement = (Element) targetDocumentsList.item(0);
                                            NodeList sourceDocumentChildren = sourceDocumentsElement.getElementsByTagName("document");
                                            
                                            for (int k = 0; k < sourceDocumentChildren.getLength(); k++) {
                                                Element sourceDocument = (Element) sourceDocumentChildren.item(k);
                                                String sourceDocName = sourceDocument.getAttribute("name");
                                                
                                                // Check if this document already exists in target
                                                boolean documentExists = false;
                                                NodeList targetDocumentChildren = targetDocumentsElement.getElementsByTagName("document");
                                                for (int l = 0; l < targetDocumentChildren.getLength(); l++) {
                                                    Element targetDocument = (Element) targetDocumentChildren.item(l);
                                                    if (sourceDocName.equals(targetDocument.getAttribute("name"))) {
                                                        documentExists = true;
                                                        break;
                                                    }
                                                }
                                                
                                                if (!documentExists) {
                                                    // Add missing document to target
                                                    Node importedDocument = targetDoc.importNode(sourceDocument, true);
                                                    targetDocumentsElement.appendChild(importedDocument);
                                                    changesMade = true;
                                                    System.out.println("Added document '" + sourceDocName + "' to existing documents element");
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
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
                    XmlUtil.updateTagsIfDiff(clonedSourceField, targetFieldMap, updatedHeaders, sourceDoc, targetDoc);
                    
                    // Fix sync and sync-with tags for existing fields
                    Element targetField = targetFieldMap.get(elementValue);
                    if (targetField != null) {
                        fixSyncTagsForField(targetField, underscoreFieldsSet, targetDoc);
                    }
                    changesMade = true;
                } else if ("header".equals(elementTag)) {
                    // Handle document elements for existing headers
                    String sourceHeaderValue = clonedSourceField.getAttribute("value");
                    
                    // Check and copy document elements from source to target header
                    NodeList sourceDocuments = clonedSourceField.getElementsByTagName("documents");
                    if (sourceDocuments.getLength() > 0) {
                        Element sourceDocumentsElement = (Element) sourceDocuments.item(0);
                        
                        // Find the corresponding header in target
                        NodeList targetHeaders = targetDoc.getElementsByTagName("header");
                        for (int j = 0; j < targetHeaders.getLength(); j++) {
                            Element targetHeader = (Element) targetHeaders.item(j);
                            if (sourceHeaderValue.equals(targetHeader.getAttribute("value"))) {
                                // Check if target header has documents element
                                NodeList targetDocumentsList = targetHeader.getElementsByTagName("documents");
                                
                                if (targetDocumentsList.getLength() == 0) {
                                    // Target header has no documents, copy from source
                                    Node importedDocuments = targetDoc.importNode(sourceDocumentsElement, true);
                                    targetHeader.appendChild(importedDocuments);
                                    changesMade = true;
                                    System.out.println("Added documents element to existing header: " + sourceHeaderValue);
                                } else {
                                    // Target header has documents, check if we need to add missing document children
                                    Element targetDocumentsElement = (Element) targetDocumentsList.item(0);
                                    NodeList sourceDocumentChildren = sourceDocumentsElement.getElementsByTagName("document");
                                    
                                    for (int k = 0; k < sourceDocumentChildren.getLength(); k++) {
                                        Element sourceDocument = (Element) sourceDocumentChildren.item(k);
                                        String sourceDocName = sourceDocument.getAttribute("name");
                                        
                                        // Check if this document already exists in target
                                        boolean documentExists = false;
                                        NodeList targetDocumentChildren = targetDocumentsElement.getElementsByTagName("document");
                                        for (int l = 0; l < targetDocumentChildren.getLength(); l++) {
                                            Element targetDocument = (Element) targetDocumentChildren.item(l);
                                            if (sourceDocName.equals(targetDocument.getAttribute("name"))) {
                                                documentExists = true;
                                                break;
                                            }
                                        }
                                        
                                        if (!documentExists) {
                                            // Add missing document to target
                                            Node importedDocument = targetDoc.importNode(sourceDocument, true);
                                            targetDocumentsElement.appendChild(importedDocument);
                                            changesMade = true;
                                            System.out.println("Added document '" + sourceDocName + "' to existing documents element");
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
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
                String isActiveValue = XmlUtil.getValue(sourceField, "is-active");
                if (isActiveValue == null || isActiveValue.trim().isEmpty()) {
                    isActiveValue = "yes";
                }
                XmlUtil.replaceOrInsertChild(clonedTemplate, "is-active", isActiveValue);
                
                String isMandatoryValue = XmlUtil.getValue(sourceField, "is-mandatory");
                if (isMandatoryValue == null || isMandatoryValue.trim().isEmpty()) {
                    isMandatoryValue = "yes";
                }
                XmlUtil.replaceOrInsertChild(clonedTemplate, "is-mandatory", isMandatoryValue);
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
                    // If field has sync tag, ensure it's active
                    XmlUtil.replaceOrInsertChild(clonedTemplate, "is-active", "yes");
                    System.out.println("Added sync tag for field: " + XmlUtil.getValue(sourceField, "db-field") + " and set is-active to yes");
                }else if (syncWithNode != null) {
                    // Fix sync-with field name to include underscore if needed
                    String syncWithValue = syncWithNode.getTextContent().trim();
                    String fixedSyncWithValue = fixSyncWithFieldName(syncWithValue, underscoreFieldsSet);
                    if (!syncWithValue.equals(fixedSyncWithValue)) {
                        syncWithNode.setTextContent(fixedSyncWithValue);
                        System.out.println("Fixed sync-with field name from: " + syncWithValue + " to: " + fixedSyncWithValue);
                    }
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

    public Node getFranchiseFeeField(){
        try {
            // Create a new document to build the field node
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            
            // Create the field element
            Element field = doc.createElement("field");
            field.setAttribute("summary", "true");
            
            // Add all child elements as specified in the user's request
            field.appendChild(XmlUtil.createElement(doc, "field-name", "_franchiseFee1181443611"));
            field.appendChild(XmlUtil.createElement(doc, "display-name", "Franchise Fee"));
            field.appendChild(XmlUtil.createElement(doc, "db-field", "_FRANCHISE_FEE_1181443611"));
            field.appendChild(XmlUtil.createElement(doc, "data-type", "String"));
            field.appendChild(XmlUtil.createElement(doc, "display-type", "Text"));
            field.appendChild(XmlUtil.createElement(doc, "db-field-length", "255"));
            
            // Create validation element
            Element validation = doc.createElement("validation");
            Element validationType = doc.createElement("validation-type");
            validationType.setTextContent("None");
            validation.appendChild(validationType);
            field.appendChild(validation);
            
            field.appendChild(XmlUtil.createElement(doc, "section", "1"));
            field.appendChild(XmlUtil.createElement(doc, "is-active", "yes"));
            field.appendChild(XmlUtil.createElement(doc, "is-mandatory", "false"));
            field.appendChild(XmlUtil.createElement(doc, "build-field", "no"));
            field.appendChild(XmlUtil.createElement(doc, "field-export", "true"));
            field.appendChild(XmlUtil.createElement(doc, "order-by", "31"));
            
            // Create mailmerge element
            Element mailmerge = doc.createElement("mailmerge");
            mailmerge.setAttribute("is-active", "true");
            mailmerge.setAttribute("keyword-name", "$franchise_franchisefee$");
            field.appendChild(mailmerge);
            
            field.appendChild(XmlUtil.createElement(doc, "pii-enabled", "false"));
            field.appendChild(XmlUtil.createElement(doc, "center-info-display", "false"));
            
            return field;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Node getIdField(){
        try {
            // Create a new document to build the field node
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            
            // Create the field element
            Element field = doc.createElement("field");
            field.setAttribute("summary", "true");
            
            // Add all child elements as specified in the user's request
            field.appendChild(XmlUtil.createElement(doc, "field-name", "centerTermsID"));
            field.appendChild(XmlUtil.createElement(doc, "display-name", "Center Terms ID"));
            field.appendChild(XmlUtil.createElement(doc, "db-field", "CENTER_TERMS_ID"));
            field.appendChild(XmlUtil.createElement(doc, "data-type", "Integer"));
            
            return field;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Node getTabPrimaryIdField(){
        try {
            // Create a new document to build the field node
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            
            // Create the field element
            Element field = doc.createElement("field");
            field.setAttribute("summary", "true");
            
            // Add all child elements as specified in the user's request
            field.appendChild(XmlUtil.createElement(doc, "field-name", "tabPrimaryId"));
            field.appendChild(XmlUtil.createElement(doc, "display-name", "Tab Primary Id"));
            field.appendChild(XmlUtil.createElement(doc, "db-field", "TAB_PRIMARY_ID"));
            field.appendChild(XmlUtil.createElement(doc, "data-type", "Integer"));
            
            return field;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void processTabularSectionMappings(String sourcePath, String targetPath) {
        try {
            Document targetDoc = XmlUtil.loadXmlDocument(targetPath);
            
            // Extract table mappings from source and target documents
            Map<String, String> sourceMappings = XmlUtil.readTableMappings(sourcePath);
            Map<String, String> targetMappings = XmlUtil.readTableMappings(targetPath);
            
            boolean changesMade = false;
            
            // Find missing mappings in target that exist in source
            for (Map.Entry<String, String> sourceEntry : sourceMappings.entrySet()) {
                String tableAnchor = sourceEntry.getKey();
                String fileLocation = sourceEntry.getValue();
                
                // Check if this mapping doesn't exist in target
                if (!targetMappings.containsKey(tableAnchor)) {
                    System.out.println("Missing table mapping found: " + tableAnchor + " -> " + fileLocation);
                    
                    // Create new table-mapping element
                    Element newMapping = targetDoc.createElement("table-mapping");
                    newMapping.setAttribute("filelocation", fileLocation);
                    newMapping.setAttribute("table-anchor", tableAnchor);
                    
                    // Append to root element
                    Element rootElement = targetDoc.getDocumentElement();
                    rootElement.appendChild(newMapping);
                    
                    changesMade = true;
                }
            }
            
            // Save the updated target document if changes were made
            if (changesMade) {
                XmlUtil.saveXmlDocument(targetDoc, targetPath);
                System.out.println("Updated tabular section mappings saved to: " + targetPath);
            } else {
                System.out.println("No missing table mappings found. Target document is up to date.");
            }
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fixes sync-with field name to include underscore prefix if the field should have underscore.
     * Format: tableName##fieldName##otherField
     * Logic:
     * 1. If fieldName already has underscore, keep it as is
     * 2. If fieldName (without underscore) is in underscoreFieldsSet, it means when this field was added,
     *    it got an underscore prefix, so the actual field in parent table likely has underscore
     * 3. Add underscore prefix if needed
     */
    private static String fixSyncWithFieldName(String syncWithValue, Set<String> underscoreFieldsSet) {
        if (syncWithValue == null || syncWithValue.trim().isEmpty()) {
            return syncWithValue;
        }
        
        // Parse sync-with format: tableName##fieldName##otherField
        String[] parts = syncWithValue.split("##");
        if (parts.length < 2) {
            return syncWithValue; // Invalid format, return as is
        }
        
        String tableName = parts[0];
        String fieldName = parts[1];
        String otherField = parts.length > 2 ? parts[2] : "false";
        
        // If field name already has underscore, no need to fix
        if (fieldName.startsWith("_")) {
            return syncWithValue;
        }
        
        // Check if field name (without underscore) is in underscoreFieldsSet
        // underscoreFieldsSet contains field names that should have underscore when added
        // So if a field name is in this set, the actual field in parent table likely has underscore
        if (underscoreFieldsSet.contains(fieldName)) {
            // Add underscore prefix
            fieldName = "_" + fieldName;
            System.out.println("Adding underscore to sync-with field name: " + fieldName + " (field was in underscoreFieldsSet)");
        }
        
        // Reconstruct the sync-with value
        return tableName + "##" + fieldName + "##" + otherField;
    }

    /**
     * Fixes sync and sync-with tags for an existing field:
     * 1. If field has sync tag, ensure it's active
     * 2. If field has sync-with tag, fix the field name to include underscore if needed
     */
    private static void fixSyncTagsForField(Element field, Set<String> underscoreFieldsSet, Document targetDoc) {
        if (field == null) {
            return;
        }
        
        // Check for sync tag
        Node syncNode = XmlUtil.getDirectChildNode(field, "sync");
        if (syncNode != null) {
            // If field has sync tag, ensure it's active
            String currentIsActive = XmlUtil.getValue(field, "is-active");
            if (!"yes".equals(currentIsActive)) {
                XmlUtil.replaceOrInsertChild(field, "is-active", "yes");
                System.out.println("Set is-active to yes for field with sync tag: " + XmlUtil.getValue(field, "db-field"));
            }
        }
        
        // Check for sync-with tag
        Node syncWithNode = XmlUtil.getDirectChildNode(field, "sync-with");
        if (syncWithNode != null) {
            String syncWithValue = syncWithNode.getTextContent().trim();
            String fixedSyncWithValue = fixSyncWithFieldName(syncWithValue, underscoreFieldsSet);
            if (!syncWithValue.equals(fixedSyncWithValue)) {
                syncWithNode.setTextContent(fixedSyncWithValue);
                System.out.println("Fixed sync-with field name in existing field: " + XmlUtil.getValue(field, "db-field") + 
                                   " from: " + syncWithValue + " to: " + fixedSyncWithValue);
            }
        }
    }
}
