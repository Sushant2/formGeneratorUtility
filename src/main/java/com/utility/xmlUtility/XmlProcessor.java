package com.utility.xmlUtility;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class XmlProcessor implements CommandLineRunner {

    @Autowired
    private XmlService xmlService;
    // /home/sushant.gupta@ad.franconnect.com/formGeneratorXml/src/main/resources/xml/tablemappings.xml
    // /home/sushant.gupta@ad.franconnect.com/builds/FCSkyPROD/config/xml/tablemappings.xml

    private static final String BASE_PATH = "src/main/resources/xml/";
    private static final String TARGET_BASE = System.getProperty("user.home") + "/builds/FCSkyPROD/config/xml/";
    /** Relative to {@link #BASE_PATH} and {@link #TARGET_BASE}. */
    private static final String EXTERNAL_FORMS_REL_PATH = "tables/externalFormBuilder/externalForms";

    /** CLIENT_XMLS-style path prefix + {@link #EXTERNAL_FORMS_REL_PATH} (leading slash). */
    private static final String EXTERNAL_FORMS_DB_PATH_PREFIX = "/" + EXTERNAL_FORMS_REL_PATH;

    private static final String EXTERNAL_FORMS_SQL_FILE = "externalForms.sql";

    public static void main(String[] args) {
        SpringApplication.run(XmlProcessor.class, args);
    }

    /**
     * XML basenames under {@code dir}, or empty set if the directory is missing or not a directory.
     */
    private static Set<String> listXmlBasenames(File dir) {
        Set<String> names = new HashSet<>();
        if (!dir.isDirectory()) {
            return names;
        }
        File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".xml"));
        if (files == null) {
            return names;
        }
        for (File f : files) {
            if (f.isFile()) {
                names.add(f.getName());
            }
        }
        return names;
    }

    @Override
    public void run(String... args) {
        try {
            Set<String> underscoreFieldsSet = new HashSet<>();

            List<String> queryList = new ArrayList<>();
            
            File sourceFormsDir = new File(BASE_PATH + EXTERNAL_FORMS_REL_PATH);
            File targetFormsDir = new File(TARGET_BASE + EXTERNAL_FORMS_REL_PATH);

            System.out.println("External forms source dir: " + sourceFormsDir.getAbsolutePath());
            System.out.println("External forms target dir: " + targetFormsDir.getAbsolutePath());

            Set<String> sourceXmlNames = listXmlBasenames(sourceFormsDir);
            Set<String> targetXmlNames = listXmlBasenames(targetFormsDir);

            if (!sourceFormsDir.isDirectory()) {
                System.err.println("Source directory does not exist or is not a directory: " + sourceFormsDir.getAbsolutePath());
            }
            if (!targetFormsDir.isDirectory()) {
                System.err.println("Target directory does not exist or is not a directory: " + targetFormsDir.getAbsolutePath());
            }

            // Every XML under source: merge into matching target when it exists, or create/populate target when it does not.
            for (String name : sourceXmlNames) {
                String sourceKeyPath = new File(sourceFormsDir, name).getPath();
                String targetKeyPath = new File(targetFormsDir, name).getPath();
                boolean both = targetXmlNames.contains(name);

                System.out.println(both ? "Processing (source + target): " + name : "Processing (source-only): " + name);
                System.out.println("  Source: " + sourceKeyPath);
                System.out.println("  Target: " + targetKeyPath);

                xmlService.processXmlFiles(sourceKeyPath, targetKeyPath, underscoreFieldsSet);
                String filePath = "";
                int index = targetKeyPath.indexOf("/tables/");
                if (index != -1) {
                    filePath = targetKeyPath.substring(index);
                }
                String query = XmlUtil.generateInsertQuery(targetKeyPath, filePath, null, underscoreFieldsSet);
                queryList.add(query);
                XmlUtil.writeToFile("src/main/resources/externalWebForms.sql", queryList);
            }

        } catch (Exception e) {
            System.err.println("Error processing xml files");
            e.printStackTrace();
        }
    }
}