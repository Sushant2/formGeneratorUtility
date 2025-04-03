package com.utility.xmlUtility;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class xmlProcessor implements CommandLineRunner {

    @Autowired
    private xmlService xmlService;

    public static void main(String[] args) {
        SpringApplication.run(xmlProcessor.class, args);
    }

    @Override
    public void run(String... args) {
		String sourceFile = "src/main/resources/franchiseembe.xml";
            String targetFile = "src/main/resources/franchiseesky.xml";
            String missingFieldsFile = "src/main/resources/missingFields.xml";
            String missingHeadersFile = "src/main/resources/missingHeaders.xml";
            String missingForeignTablesFile = "src/main/resources/missingForeignTables.xml";

            xmlService.processXmlFiles(sourceFile, targetFile, missingFieldsFile, missingHeadersFile, missingForeignTablesFile);
	}
}