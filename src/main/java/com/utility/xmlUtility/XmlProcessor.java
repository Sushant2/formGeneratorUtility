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
            
            Set<String> requiredKeySet = new HashSet<>(Arrays.asList("address","address_copy","areaCall","areaCall_copy","areaContract","areaContractExport","areaContractExport_copy","areaContract_copy","areaEntityDetail","areaEntityDetailExport","areaEntityDetailExport_copy","areaEntityDetail_copy","areaEvents","areaEvents_copy","areaFinancial","areaFinancialExport","areaFinancialExport_copy","areaFinancial_copy","areaGuarantor","areaGuarantorExport","areaGuarantorExport_copy","areaGuarantor_copy","areaInfo","areaInfo_copy","areaInsurance","areaInsuranceExport","areaInsuranceExport_copy","areaInsurance_copy","areaLegalViolation","areaLegalViolation_copy","areaLender","areaLenderExport","areaLenderExport_copy","areaLender_copy","areaLicenseAgreement","areaLicenseAgreementExport","areaLicenseAgreementExport_copy","areaLicenseAgreement_copy","areaMarketing","areaMarketingExport","areaMarketingExport_copy","areaMarketing_copy","areaMysteryShopper","areaMysteryShopper_copy","areaOwners","areaOwners_copy","areaQa","areaQaExport","areaQaExport_copy","areaQa_copy","areaRealEstate","areaRealEstateExport","areaRealEstateExport_copy","areaRealEstate_copy","areaRenewal","areaRenewal_copy","areaTasks","areaTasks_copy","areaTerritory","areaTerritoryExport","areaTerritoryExport_copy","areaTerritory_copy","areaTraining","areaTrainingExport","areaTrainingExport_copy","areaTraining_copy","areaUsers","areaUsers_copy","areas","areas_copy","callStatus","callStatus_copy","callType_copy","centerInfoDisplay","centerInfoDisplay_copy","entityCall","entityCall_copy","entityDisplayDetail","entityDisplayDetail_copy","externalWebFormsApproval","externalWebFormsApproval_copy","fimAddress","fimAddressExport","fimAddressExport_copy","fimAddress_copy","fimAgreementVersionsExport","fimAgreementVersionsExport_copy","fimAreaRemarks","fimAreaRemarks_copy","fimBrandMapping","fimBrandMapping_copy","fimBuilderField","fimBuilderField_copy","fimCampaign","fimCampaignEmailCampaign","fimCampaignEmailCampaign_copy","fimCampaignTemplates","fimCampaignTemplates_copy","fimCampaign_copy","fimCapturePopServer","fimCapturePopServer_copy","fimComplaint","fimComplaintExport","fimComplaintExport_copy","fimComplaint_copy","fimConfigureOptOutMessage","fimConfigureOptOutMessage_copy","fimContract","fimContractAdditional","fimContractAdditional_copy","fimContractExport","fimContractExport_copy","fimContract_copy","fimCustomTab","fimCustomTabFields","fimCustomTabFields_copy","fimCustomTabSections","fimCustomTabSections_copy","fimCustomTab_copy","fimDocuments","fimDocuments_copy","fimEmployees","fimEmployeesExport","fimEmployeesExport_copy","fimEmployeesMapping","fimEmployeesMappingExport","fimEmployeesMappingExport_copy","fimEmployeesMapping_copy","fimEmployees_copy","fimEntityDetail","fimEntityDetailExport","fimEntityDetailExport_copy","fimEntityDetail_copy","fimEntityLocationMapping","fimEntityLocationMapping_copy","fimEntityOwnerMapping","fimEntityOwnerMapping_copy","fimEvents","fimEvents_copy","fimExternalMail","fimExternalMail_copy","fimFinancial","fimFinancialExport","fimFinancialExport_copy","fimFinancialIfFinancialsExport","fimFinancialIfFinancialsExport_copy","fimFinancial_copy","fimFranchiseAgreementVersionsExport","fimFranchiseAgreementVersionsExport_copy","fimFranchiseeEmail","fimFranchiseeEmail_copy","fimGroups","fimGroupsArchived","fimGroupsArchived_copy","fimGroups_copy","fimGuarantor","fimGuarantorExport","fimGuarantorExport_copy","fimGuarantor_copy","fimInsurance","fimInsuranceExport","fimInsuranceExport_copy","fimInsurance_copy","fimLegalViolation","fimLegalViolation_copy","fimLender","fimLenderExport","fimLenderExport_copy","fimLender_copy","fimLicenseAgreement","fimLicenseAgreementExport","fimLicenseAgreementExport_copy","fimLicenseAgreement_copy","fimMarketing","fimMarketingExport","fimMarketingExport_copy","fimMarketing_copy","fimMuContract","fimMuContractExport","fimMuContractExport_copy","fimMuContract_copy","fimMuDocuments","fimMuDocuments_copy","fimMuEntityDetail","fimMuEntityDetailExport","fimMuEntityDetailExport_copy","fimMuEntityDetail_copy","fimMuEvents","fimMuEvents_copy","fimMuInfo","fimMuInfo_copy","fimMuLegalViolation","fimMuLegalViolation_copy","fimMuLicenseAgreement","fimMuLicenseAgreementExport","fimMuLicenseAgreementExport_copy","fimMuLicenseAgreement_copy","fimMuMarketing","fimMuMarketingExport","fimMuMarketingExport_copy","fimMuMarketing_copy","fimMuOtherAddress","fimMuOtherAddressExport","fimMuOtherAddressExport_copy","fimMuOtherAddress_copy","fimMuOwners","fimMuOwnersExport","fimMuOwnersExport_copy","fimMuOwners_copy","fimMuRealEstate","fimMuRealEstateExport","fimMuRealEstateExport_copy","fimMuRealEstate_copy","fimMuRemarks","fimMuRemarks_copy","fimMuTerritory","fimMuTerritoryExport","fimMuTerritoryExport_copy","fimMuTerritory_copy","fimMysteryShopper","fimMysteryShopper_copy","fimOwners","fimOwnersExport","fimOwnersExport_copy","fimOwners_copy","fimPicture","fimPictureExport","fimPictureExport_copy","fimPicture_copy","fimQa","fimQaExport","fimQaExport_copy","fimQa_copy","fimReacquiring","fimReacquiring_copy","fimRealEstate","fimRealEstateExport","fimRealEstateExport_copy","fimRealEstate_copy","fimRenewal","fimRenewal_copy","fimSCFranchiseToDoList","fimSCFranchiseToDoList_copy","fimSCToDoList","fimSCToDoList_copy","fimTasks","fimTasks_copy","fimTemplates","fimTemplates_copy","fimTermination","fimTerminationExport","fimTerminationExport_copy","fimTermination_copy","fimTerritory","fimTerritoryExport","fimTerritoryExport_copy","fimTerritory_copy","fimTraining","fimTrainingCourseExport","fimTrainingCourseExport_copy","fimTrainingParticipantExport","fimTrainingParticipantExport_copy","fimTrainingQuizParticipantExport","fimTrainingQuizParticipantExport_copy","fimTrainingUsersExport","fimTrainingUsersExport_copy","fimTraining_copy","fimTransfer","fimTransferExport","fimTransferExport_copy","fimTransferStatus","fimTransferStatus_copy","fimTransfer_copy","fimUsers","fimUsers_copy","fimfranchiseeMapping","fimfranchiseeMapping_copy","fimfranchiseeRemarks","fimfranchiseeRemarks_copy","franchiseeCall","franchiseeCall_copy","franchiseeExport","franchiseeExport_copy","franchiseeLocalListings","franchiseeLocalListings_copy","franchiseeMailmergeTemplateRel","franchiseeMailmergeTemplateRel_copy","franchisees","franchiseesExport","franchiseesExport_copy","franchisees_copy","fsFranchiseDevelopment","fsFranchiseDevelopment_copy","fsFranchiseeQualification","fsFranchiseeQualification_copy","fsLeadBusinessProfile","fsLeadBusinessProfile_copy","fsLeadCompliance","fsLeadComplianceAdditional","fsLeadComplianceAdditional_copy","fsLeadCompliance_copy","fsLeadDetails","fsLeadDetailsExport","fsLeadDetailsExport_copy","fsLeadDetails_copy","fsLeadPersonalProfile","fsLeadPersonalProfile_copy","fsLeadQualification","fsLeadQualificationDetail","fsLeadQualificationDetail_copy","fsLeadQualification_copy","fsLeadRating","fsLeadRating_copy","fsLeadRealEstate","fsLeadRealEstate_copy","fsLeadSMS","fsLeadSMS_copy","fsLeadSchedule","fsLeadScheduleVisitors","fsLeadScheduleVisitors_copy","fsLeadSchedule_copy","fsSiteLocation","fsSiteLocation_copy","fsSmsTemplates","fsSmsTemplates_copy","fsSubscriptionLogs","fsSubscriptionLogs_copy","fsSubscriptionMailData","fsSubscriptionMailData_copy","fsSubscriptionSMSData","fsSubscriptionSMSData_copy","fsTaskTriggers","fsTaskTriggers_copy","fsTasks","fsTasks_copy","fsVisitTaskMapping","fsVisitTaskMapping_copy","fsleadCall","fsleadCall_copy","muCall","muCall_copy","muDetails","muDetailsExport","muDetailsExport_copy","muDetails_copy","muFimOwners","muFimOwners_copy","muFinancial","muFinancialExport","muFinancialExport_copy","muFinancial_copy","muGuarantor","muGuarantorExport","muGuarantorExport_copy","muGuarantor_copy","muInsurance","muInsuranceExport","muInsuranceExport_copy","muInsurance_copy","muLender","muLenderExport","muLenderExport_copy","muLender_copy","muMysteryShopper","muMysteryShopper_copy","muOutlookMailAttachments","muOutlookMailAttachments_copy","muOutlookMails","muOutlookMailsExport","muOutlookMailsExport_copy","muOutlookMails_copy","muOwners","muOwnersExport","muOwnersExport_copy","muOwners_copy","muQa","muQaExport","muQaExport_copy","muQa_copy","muRenewal","muRenewal_copy","owners","ownersExport","ownersExport_copy","owners_copy"));

            Set<String> customFieldTabSet = new HashSet<>(Arrays.asList("fsLeadRealEstate","fsLeadSchedule","fsLeadDetailsExport","fsLeadCompliance","fsLeadScheduleVisitors","fsTasks","fsLeadBusinessProfile","fsSubscriptionSMSData","fsLeadQualificationDetail","fsSubscriptionMailData","fsFranchiseDevelopment","fsleadCall","fsLeadQualification","fsFranchiseeQualification","fsLeadRating","fsLeadComplianceAdditional","fsVisitTaskMapping","fsLeadSMS","fsSubscriptionLogs","fsLeadPersonalProfile","fsSmsTemplates","fsLeadDetails"));

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

                    // Generate query for tabmodules.xml
                    // String tabModulesQuery = XmlUtil.generateInsertQuery(targetKeyPath, filePath, null);
                    // tabModulesInnerXMLsQueryList.add(tabModulesQuery);

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
                        // String innerXMLQuery = XmlUtil.generateInsertQuery(completeTargetInnerXmlPath, innerXmlPath, moduleName);
                        // tabModulesInnerXMLsQueryList.add(innerXMLQuery);
                    }
                    // Writing queries into file
                    XmlUtil.writeToFile("src/main/resources/tabModulesQueries.sql", tabModulesInnerXMLsQueryList);
                    continue;
                }

                // Process the XML files
                if(customFieldTabSet.contains(key)) {
                    System.out.println("============>>>>>>>>>>>" + key);
                    xmlService.processXmlFiles(targetKeyPath, targetKeyPath);
                    // Delete & Insert Query
                    // String query = XmlUtil.generateInsertQuery(targetKeyPath, filePath, null);
                    // queryList.add(query);

                    // Writing queries into file
                    // XmlUtil.writeToFile("src/main/resources/tableMappingsQueries.sql", queryList);
                }
            }
            // xmlService.processXmlFiles("src/main/resources/mbe.xml", "src/main/resources/sky.xml");

        } catch (Exception e) {
            System.err.println("Error processing xml files");
            e.printStackTrace();
        }
    }
}