package com.utility.xmlUtility;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@SpringBootApplication
public class XmlProcessor implements CommandLineRunner {

    @Autowired
    private XmlService xmlService;
    // /home/sushant.gupta@ad.franconnect.com/formGeneratorXml/src/main/resources/xml/tablemappings.xml
    // /home/sushant.gupta@ad.franconnect.com/builds/FCSkyPROD/config/xml/tablemappings.xml

    private static final String BASE_PATH = "src/main/resources/xml/";
    private static final String TARGET_BASE = System.getProperty("user.home") + "/builds/FCSkyPROD/config/xml/";

    public static void main(String[] args) {
        SpringApplication.run(XmlProcessor.class, args);
    }

    @Override
    public void run(String... args) {
        try {

            List<String> insertQueries = new ArrayList<>();

            String sourcePath = BASE_PATH + "/tablemappings.xml" + "/";
            String targetPath = TARGET_BASE + "/tablemappings.xml" + "/";

            System.out.println("Source Path: " + sourcePath);
            System.out.println("Target Path: " + targetPath);

            Map<String, String> sourceTableMappingsMap = XmlUtil.readTableMappings(sourcePath);
            Map<String, String> targetTableMappingsMap = XmlUtil.readTableMappings(targetPath);

            for (Map.Entry<String, String> entry : sourceTableMappingsMap.entrySet()) {
                System.out.println("Key: " + entry.getKey() + " → Value: " + entry.getValue());
            }

            System.out.println("-----------------------------------------------------");

            for (Map.Entry<String, String> entry : targetTableMappingsMap.entrySet()) {
                System.out.println("Key: " + entry.getKey() + " → Value: " + entry.getValue());
            }

            for(String key: sourceTableMappingsMap.keySet()) { // e.g. "fimCompliance"
                if(!targetTableMappingsMap.containsKey(key)) {
                    System.out.println("Key: " + key + " is missing in target XML");
                    //String insertSourceQuery = XmlUtil.generateInsertQuery(BASE_PATH + sourceTableMappingsMap.get(key), sourceTableMappingsMap.get(key));
                    //insertQueries.add(insertSourceQuery);
                    continue;
                }

                // //Extracting key from the targetTableMappingsMap
                String targetTableMappingsMapKey = targetTableMappingsMap.get(key);

                String sourceKeyPath = BASE_PATH + sourceTableMappingsMap.get(key);
                String targetKeyPath = TARGET_BASE + targetTableMappingsMapKey;

                // Check if files exist before proceeding
                File sourceFile = new File(sourceKeyPath);
                File targetFile = new File(targetKeyPath);

                if (!sourceFile.exists() || !targetFile.exists()) {
                    System.out.println("Skipping key: " + key + " → Missing file!");
                    if (!sourceFile.exists()) System.out.println("Source XML file missing: " + sourceKeyPath);
                    if (!targetFile.exists()) System.out.println("Target XML file missing: " + targetKeyPath);
                    continue;
                }

                System.out.println("Processing XML files... Source: " + sourceKeyPath + " Target: " + targetKeyPath);
                //for tabModules.xml

                if(sourceKeyPath.contains("tabmodules.xml") && targetKeyPath.contains("tabmodules.xml")){
                    Map<String, Element> sourceTabModuleElements = XmlUtil.readTabModules(sourceKeyPath);
                    Map<String, Element> targetTabModuleElements = XmlUtil.readTabModules(targetKeyPath);
                   
                    XmlUtil.processCustomModulesXml(sourceTabModuleElements, targetTabModuleElements, sourceKeyPath, targetKeyPath);
                    //Now read the updated target xml and process it
                    Document targetTabModulesDoc = XmlUtil.loadXmlDocument(targetKeyPath);
                    NodeList moduleTabs = targetTabModulesDoc.getElementsByTagName("module-tab");
                    for (int i = 0; i < moduleTabs.getLength(); i++) {
                        Element moduleTab = (Element) moduleTabs.item(i);
                        String tabName = moduleTab.getAttribute("tab-name");
                        String innerXmlPath = moduleTab.getAttribute("fileLocation");
                        System.out.println("Processing tab: " + tabName + " with inner XML path: " + innerXmlPath);

                        // Build the correct Source and Target path
                        String basePath = BASE_PATH.endsWith("/") ? BASE_PATH.substring(0, BASE_PATH.length() - 1) : BASE_PATH; ///remove last "/"
                        String sourceInnerXmlPath = basePath + (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);
                        String targetInnerXmlPath = "/tabModulesXml" + (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);

                        System.out.println("Reading from source: " + sourceInnerXmlPath);
                        System.out.println("Writing to target: " + targetInnerXmlPath);

                        xmlService.processXmlFiles(sourceInnerXmlPath, targetInnerXmlPath);
                    }
                    continue;
                }

                xmlService.processXmlFiles(sourceKeyPath, targetKeyPath);

                // Generate query
                String filePath = "";
                if(targetTableMappingsMapKey.startsWith("/"))
                    filePath = targetTableMappingsMapKey;   
                else
                    filePath = "/" + targetTableMappingsMapKey;          // e.g. "/tables/admin/franchiseesky.xml"
                // String insertQuery = XmlUtil.generateInsertQuery(targetKeyPath, filePath);
                
                String xmlFilename = new File(targetKeyPath).getName();           // e.g. "franchiseesky.xml"
                String module = "";
                String data = new String(Files.readAllBytes(Paths.get(targetKeyPath)), StandardCharsets.UTF_8)
                                .replace("'", "''"); // escape single quotes for SQL

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

                StringBuilder query = new StringBuilder();
                //delete query for xmlkey
                query.append("DELETE FROM CLIENT_XMLS WHERE XML_KEY = '").append(xmlKey).append("';");
                query.append(System.lineSeparator());

                //delete query for xmlkey_copy
                query.append("DELETE FROM CLIENT_XMLS WHERE XML_KEY = '").append(xmlKey).append("_copy").append("';");

                query.append(System.lineSeparator());

                //insert query for xmlkey
                query.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
                query.append("NULL, ");
                query.append("'").append(xmlFilename).append("', ");
                query.append("'").append(xmlKey).append("', ");
                query.append("'").append(module).append("', ");
                query.append("'").append(filePath).append("', ");
                query.append("'").append(data).append("', ");
                query.append("CURRENT_TIMESTAMP);");

                query.append(System.lineSeparator());

                //insert query for xmlkey_copy
                String copiedXmlFilename = xmlFilename.replace(".xml", "_copy.xml");
                String copiedFilePath = filePath.replace(".xml", "_copy.xml");

                query.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
                query.append("NULL, ");
                query.append("'").append(copiedXmlFilename).append("', ");
                query.append("'").append(xmlKey).append("_copy").append("', ");
                query.append("'").append(module).append("', ");
                query.append("'").append(copiedFilePath).append("', ");
                query.append("'").append(data).append("', ");
                query.append("CURRENT_TIMESTAMP);");
                
                String insertQuery = query.toString();

                System.out.println("Generated Query: " + insertQuery);
                                
                insertQueries.add(insertQuery);

                File outputFile = new File("src/main/resources/sql_queries.sql");
                outputFile.getParentFile().mkdirs();
                // Create the file if it doesn't exist
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
                    for (String currQuery : insertQueries) {
                        if(currQuery == null || currQuery.isEmpty()) {
                            System.out.println("Skipping empty query");
                            continue;
                        }
                        writer.write(currQuery);
                        writer.newLine();
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error writing to file: " + e.getMessage());
                }
                
            }
            // xmlService.processXmlFiles("src/main/resources/mbe.xml", "src/main/resources/sky.xml");

        } catch (Exception e) {
            System.err.println("Error processing xml files");
            e.printStackTrace();
        }
    }
}