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

            String sourcePath = BASE_PATH + "tablemappings.xml" + "/";
            String targetPath = TARGET_BASE + "tablemappings.xml" + "/";

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

            for(String key: sourceTableMappingsMap.keySet()) {
                if(!targetTableMappingsMap.containsKey(key)) {
                    System.out.println("Key: " + key + " is missing in target XML");
                    continue;
                }
                String sourceKeyPath = sourceTableMappingsMap.get(key);
                String targetKeyPath = targetTableMappingsMap.get(key);
                System.out.println("Processing XML files... Source: " + sourceKeyPath + " Target: " + targetKeyPath);
                // xmlService.processXmlFiles(sourceKeyPath, targetKeyPath);

                 // Generate query
                String xmlFilename = new File(targetKeyPath).getName();           // e.g. "franchiseesky.xml"
                String filelocation = targetTableMappingsMap.get(key);            // e.g. "tables/admin/franchiseesky.xml"
                String data = new String(Files.readAllBytes(Paths.get(targetKeyPath)), StandardCharsets.UTF_8)
                                .replace("'", "''"); // escape single quotes for SQL

                StringBuilder query = new StringBuilder();
                query.append("INSERT INTO CLIENT_XMLS(ID, NAME, XML_KEY, MODULE, FILE_PATH, DATA, LAST_MODIFIED) VALUES (");
                query.append("NULL, ");
                query.append("'").append(xmlFilename).append("', ");
                query.append("'").append(key).append("', ");
                query.append("'admin', ");
                query.append("'").append(filelocation).append("', ");
                query.append("'").append(data).append("', ");
                query.append("CURRENT_TIMESTAMP);");
                
                String insertQuery = query.toString();
                                
                insertQueries.add(insertQuery);

                File outputFile = new File("src/main/resources/sql_queries.sql");
                outputFile.getParentFile().mkdirs();
                // Create the file if it doesn't exist
                if (!outputFile.exists()) {
                    outputFile.createNewFile();
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, StandardCharsets.UTF_8))) {
                    for (String currQuery : insertQueries) {
                        writer.write(currQuery);
                        writer.newLine();
                    }
                }catch (Exception e) {
                    System.err.println("Error writing to file: " + e.getMessage());
                }
                
            }
            // xmlService.processXmlFiles("src/main/resources/franchiseembe.xml", "src/main/resources/franchiseesky.xml");

        } catch (Exception e) {
            System.err.println("Error processing xml files");
            e.printStackTrace();
        }
    }
}