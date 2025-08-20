package com.utility.xmlUtility;

import java.io.File;
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

    public static void main(String[] args) {
        SpringApplication.run(XmlProcessor.class, args);
    }

    @Override
    public void run(String... args) {
        try {

            List<String> queryList = new ArrayList<>();

            Set<String> requiredKeySet = XmlUtil.getRequiredKeySet();
            Set<String> underscoreFieldsSet = new HashSet<>();
            String sourcePath = BASE_PATH + "/tablemappings.xml" + "/";
            String targetPath = TARGET_BASE + "/tablemappings.xml" + "/";

            System.out.println("Source Path: " + sourcePath);
            System.out.println("Target Path: " + targetPath);

            Map<String, String> sourceTableMappingsMap = XmlUtil.readTableMappings(sourcePath);
            Map<String, String> targetTableMappingsMap = XmlUtil.readTableMappings(targetPath);

            /*for (Map.Entry<String, String> entry : sourceTableMappingsMap.entrySet()) {
                System.out.println("Key: " + entry.getKey() + " → Value: " + entry.getValue());
            }

            System.out.println("-----------------------------------------------------");

            for (Map.Entry<String, String> entry : targetTableMappingsMap.entrySet()) {
                System.out.println("Key: " + entry.getKey() + " → Value: " + entry.getValue());
            }*/

            for(String key: sourceTableMappingsMap.keySet()) { // e.g. "fimCompliance"
                if(!targetTableMappingsMap.containsKey(key)) {
                    System.out.println("Key: " + key + " is missing in target XML");
                    //String insertSourceQuery = XmlUtil.generateInsertQuery(BASE_PATH + sourceTableMappingsMap.get(key), sourceTableMappingsMap.get(key));
                    //queryList.add(insertSourceQuery);
                    continue;
                }

                // Extracting key from the targetTableMappingsMap
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

                // Determine the file path for the target XML
                String filePath = "";
                if(targetTableMappingsMapKey.startsWith("/"))
                    filePath = targetTableMappingsMapKey;   
                else
                    filePath = "/" + targetTableMappingsMapKey;          // e.g. "/tables/admin/franchiseesky.xml"

                //for tabModules.xml
                if (sourceKeyPath.contains("tabmodules.xml") && targetKeyPath.contains("tabmodules.xml")) {
                    Map<String, Element> sourceTabModuleElements = XmlUtil.readTabModules(sourceKeyPath);
                    Map<String, Element> targetTabModuleElements = XmlUtil.readTabModules(targetKeyPath);

                    List<String> tabModulesInnerXMLsQueryList = new ArrayList<>();

                    XmlUtil.processCustomModulesXml(sourceTabModuleElements, targetTabModuleElements, sourceKeyPath,
                            targetKeyPath);

                    // Generate query for tabmodules.xml
                    String tabModulesQuery = XmlUtil.generateInsertQuery(targetKeyPath, filePath, null, underscoreFieldsSet);
                    tabModulesInnerXMLsQueryList.add(tabModulesQuery);

                    // Writing queries into file
                    XmlUtil.writeToFile("src/main/resources/tabModulesQueries.sql", tabModulesInnerXMLsQueryList);

                    // Now read the updated target XML and process it
                    Document targetTabModulesDoc = XmlUtil.loadXmlDocument(targetKeyPath);
                    NodeList moduleTabs = targetTabModulesDoc.getElementsByTagName("module-tab");
                    for (int i = 0; i < moduleTabs.getLength(); i++) {
                        Element moduleTab = (Element) moduleTabs.item(i);
                        String tabName = moduleTab.getAttribute("tab-name");
                        String innerXmlPath = moduleTab.getAttribute("fileLocation");
                        String moduleName = moduleTab.getAttribute("module");
                        System.out.println("Processing tab: " + tabName + " with inner XML path: " + innerXmlPath);

                        // Build the correct Source and Target path
                        String sourceInnerXmlPath = BASE_PATH
                                + (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);
                        String targetInnerXmlPath = "/tabModulesXml"
                                + (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);

                        System.out.println("Reading from source: " + sourceInnerXmlPath);
                        System.out.println("Writing to target: " + targetInnerXmlPath);

                        xmlService.processXmlFiles(sourceInnerXmlPath, targetInnerXmlPath, underscoreFieldsSet);
                        // Generate query for inner XMLs of tabmodules.xml
                        String completeTargetInnerXmlPath = System.getProperty("user.home")
                                + "/formGeneratorXml/src/main/resources/" + targetInnerXmlPath;
                        innerXmlPath = (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);
                        String innerXMLQuery = XmlUtil.generateInsertQuery(completeTargetInnerXmlPath, innerXmlPath,
                                moduleName, underscoreFieldsSet);
                        tabModulesInnerXMLsQueryList.add(innerXMLQuery);
                    }
                    // Writing queries into file
                    XmlUtil.writeToFile("src/main/resources/tabModulesQueries.sql", tabModulesInnerXMLsQueryList);
                    continue;
                }

                if(requiredKeySet.contains(key)) {

                    if(key.equals("fimEntityDetail") || key.equals("fimEntityDetail_copy")) {
                        String query = XmlUtil.getSpecificXmlQuery("fimEntityDetail");
                        queryList.add(query);
                    }
                    else if(key.equals("fimTraining") || key.equals("fimTraining_copy")){
                        String query = XmlUtil.getSpecificXmlQuery("fimTraining");
                        queryList.add(query);
                    }else{
                        // Process the XML files
                        xmlService.processXmlFiles(sourceKeyPath, targetKeyPath, underscoreFieldsSet);

                        // Delete & Insert Query
                        String query = XmlUtil.generateInsertQuery(targetKeyPath, filePath, null, underscoreFieldsSet);
                        queryList.add(query);

                        if(key.equals("franchisees") || key.equals("franchisees_copy")){
                            query = XmlUtil.getSpecificXmlQuery("franchisees");
                            queryList.add(query);
                        }
                    }

                    // Writing queries into file
                    XmlUtil.writeToFile("src/main/resources/tableMappingsQueries.sql", queryList);
                }
            }
            
            // For testing purpose only - single XML
            /*xmlService.processXmlFiles("src/main/resources/mbe.xml", "src/main/resources/sky.xml", underscoreFieldsSet);
            String query = XmlUtil.generateInsertQuery("src/main/resources/sky.xml", "filePath", null, underscoreFieldsSet);
            queryList.add(query);
            XmlUtil.writeToFile("src/main/resources/sampleQueries.sql", queryList);*/

        } catch (Exception e) {
            System.err.println("Error processing xml files");
            e.printStackTrace();
        }
    }
}