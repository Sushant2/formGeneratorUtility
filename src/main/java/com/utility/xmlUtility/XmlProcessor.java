package com.utility.xmlUtility;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

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

            Map<String, String> sourceTableMappingsMap = readTableMappings(sourcePath);
            Map<String, String> targetTableMappingsMap = readTableMappings(targetPath);

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
                String sourceKeyPath = BASE_PATH + sourceTableMappingsMap.get(key);
                String targetKeyPath = TARGET_BASE + targetTableMappingsMap.get(key);

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
                xmlService.processXmlFiles(sourceKeyPath, targetKeyPath);

                 // Generate query
                String filePath = "/" + targetTableMappingsMap.get(key);            // e.g. "/tables/admin/franchiseesky.xml"
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