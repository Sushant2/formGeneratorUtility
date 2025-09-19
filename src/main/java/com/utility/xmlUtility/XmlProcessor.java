package com.utility.xmlUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

            String customFieldHeader = "Module, Sub-Module, Display Type, Display Name, Previous Field Name, New Field Name";
            XmlUtil.appendToCustomFieldsCSV(customFieldHeader);
            List<String> queryList = new ArrayList<>();

            // update the custom field set based on sales or fim query result
            /*FIM :
           SELECT GROUP_CONCAT(CONCAT('"', XML_KEY, '"') ORDER BY XML_KEY) FROM CLIENT_XMLS WHERE MODULE IN ('fim', 'admin') AND XML_KEY NOT LIKE '%copy' AND XML_KEY NOT LIKE '%Export' AND XML_KEY != 'centerInfoDisplay' AND XML_KEY NOT LIKE 'mu%' AND XML_KEY NOT LIKE 'fimMu%' AND XML_KEY NOT LIKE 'area%' AND XML_KEY NOT LIKE 'fimArea%' AND XML_KEY NOT IN ('fimTerritory','fimGroupsArchived','fimLender','fimMarketing','fimMysteryShopper','fimPicture','fimQa','fimReacquiring','fimRealEstate','fimTermination','fimUsers','fimGroups','fimInsurance','fimLegalViolation','fimGuarantor','fimContract','fimEmployees','fimEntityDetail','fimCampaign','fimConfigureOptOutMessage','fimCustomTab','fimCustomTabFields','fimCustomTabSections','fimSCToDoList','fimEntityOwnerMapping','fimSCFranchiseToDoList','fimEntityLocationMapping','entityDisplayDetail','entityCall','fimCapturePopServer','fimBrandMapping','fimAddress','fimLicenseAgreement','fimDocuments','fimfranchiseeMapping''fimCampaignTemplates','fimCampaignEmailCampaign','fimContractAdditional''fimEmployeesMapping','fimBuilderField','fimCampaignTemplates','fimContractAdditional','fimEmployeesMapping','fimfranchiseeMapping','tabmodules');

            SALES :
            SELECT GROUP_CONCAT(CONCAT('"', XML_KEY, '"')) FROM CLIENT_XMLS WHERE MODULE IN ('fs', 'fsales') AND XML_KEY NOT LIKE '%copy' AND XML_KEY NOT LIKE '%Export' AND XML_KEY NOT IN ('fsLeadSchedule','fsLeadScheduleVisitors','fsLeadComplianceAdditional','fsVisitTaskMapping','fsLeadSMS','fsSubscriptionSMSData','fsSmsTemplates','fsBqual','fsLeadBusinessProfile','fsSubscriptionMailData','fsFranchiseDevelopment','fsLeadRating','fsSubscriptionLogs','fsLeadQualification','fsFranchiseeQualification');
            */

            Set<String> customFieldTabSet = new HashSet<>(Arrays.asList());

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

                    XmlUtil.processCustomModulesXml(sourceTabModuleElements, targetTabModuleElements, sourceKeyPath, targetKeyPath);

                    // Writing queries into file
                    XmlUtil.writeToFile("src/main/resources/tabModulesQueries.sql", tabModulesInnerXMLsQueryList);

                    // Now read the updated target xml and process it
                    Document targetTabModulesDoc = XmlUtil.loadXmlDocument(targetKeyPath);
                    NodeList moduleTabs = targetTabModulesDoc.getElementsByTagName("module-tab");
                    for (int i = 0; i < moduleTabs.getLength(); i++) {
                        Element moduleTab = (Element) moduleTabs.item(i);
                        String tabName = moduleTab.getAttribute("tab-name");
                        String innerXmlPath = moduleTab.getAttribute("fileLocation");
                        String moduleName = moduleTab.getAttribute("module");
                        System.out.println("Processing tab: " + tabName + " with inner XML path: " + innerXmlPath);

                        // Build the correct Source and Target path
                        String basePath = BASE_PATH.endsWith("/") ? BASE_PATH.substring(0, BASE_PATH.length() - 1)
                                : BASE_PATH; /// remove last "/"
                        String sourceInnerXmlPath = basePath
                                + (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);
                        String targetInnerXmlPath = "/tabModulesXml"
                                + (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);

                        System.out.println("Reading from source: " + sourceInnerXmlPath);
                        System.out.println("Writing to target: " + targetInnerXmlPath);
                        if(customFieldTabSet.contains(tabName)) {
                            xmlService.processXmlFiles(sourceInnerXmlPath, targetInnerXmlPath);
                        }
                        // Generate query for inner XMLs of tabmodules.xml
                        String completeTargetInnerXmlPath = System.getProperty("user.home")
                                + "/formGeneratorXml/src/main/resources/" + targetInnerXmlPath;
                        innerXmlPath = (innerXmlPath.startsWith("/") ? innerXmlPath : "/" + innerXmlPath);
                    }
                    // Writing queries into file
                    XmlUtil.writeToFile("src/main/resources/tabModulesQueries.sql", tabModulesInnerXMLsQueryList);
                    continue;
                }

                // Process the XML files
                if(customFieldTabSet.contains(key)) {
                    xmlService.processXmlFiles(sourceKeyPath, targetKeyPath);
                }
            }

        } catch (Exception e) {
            System.err.println("Error processing xml files");
            e.printStackTrace();
        }
    }
}