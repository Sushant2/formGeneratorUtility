package com.utility.xmlUtility;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads {@code customFIeldsList.csv} once and resolves field names where a row index
 * {@code _<digits>} was concatenated without a separating underscore before a known custom field.
 */
public final class CustomFieldNamesRegistry {

    private static final String CSV_RESOURCE = "customFIeldsList.csv";
    private static final String CSV_PROJECT_RELATIVE = "src/main/resources/customFIeldsList.csv";
    private static final Pattern ROW_INDEX = Pattern.compile("_\\d+");
    private static final Pattern NEXT_ROW_INDEX = Pattern.compile("_\\d+");

    private static volatile Set<String> customFieldNames = null;

    private CustomFieldNamesRegistry() {
    }

    /**
     * @return unmodifiable set of names from the CSV (never null; empty if file missing)
     */
    public static Set<String> getCustomFieldNames() {
        Set<String> local = customFieldNames;
        if (local != null) {
            return local;
        }
        synchronized (CustomFieldNamesRegistry.class) {
            if (customFieldNames == null) {
                customFieldNames = Collections.unmodifiableSet(loadFromDisk());
            }
            return customFieldNames;
        }
    }

    private static Set<String> loadFromDisk() {
        Set<String> names = new HashSet<>();
        try {
            Path path = Paths.get(CSV_PROJECT_RELATIVE);
            if (Files.isRegularFile(path)) {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    addLine(names, line);
                }
                return names;
            }
        } catch (Exception ignored) {
            // fall through to classpath
        }
        try (InputStream in = CustomFieldNamesRegistry.class.getClassLoader().getResourceAsStream(CSV_RESOURCE)) {
            if (in == null) {
                return names;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                addLine(names, line);
            }
        } catch (Exception ignored) {
            // leave names empty
        }
        return names;
    }

    private static void addLine(Set<String> names, String line) {
        if (line == null) {
            return;
        }
        String t = line.trim();
        if (t.isEmpty() || t.startsWith("#")) {
            return;
        }
        names.add(t);
    }

    /**
     * After each {@code _<digits>} run, if the next character is not {@code '_'}, inserts {@code '_'}
     * before the following custom-field token when that token appears in the CSV list.
     */
    public static String normalizeFieldNameAfterRowIndex(String fieldName) {
        if (fieldName == null || fieldName.isEmpty()) {
            return fieldName;
        }
        Set<String> known = getCustomFieldNames();
        if (known.isEmpty()) {
            return fieldName;
        }
        String s = fieldName;
        boolean progress = true;
        while (progress) {
            progress = false;
            Matcher m = ROW_INDEX.matcher(s);
            while (m.find()) {
                int end = m.end();
                if (end >= s.length() || s.charAt(end) == '_') {
                    continue;
                }
                String matchedTail = tailSegmentMatchingCustomField(s, end, known);
                if (matchedTail != null) {
                    s = s.substring(0, end) + "_" + s.substring(end);
                    progress = true;
                    break;
                }
            }
        }
        return s;
    }

    /**
     * {@code s} has a row index ending at {@code startOfTail}; returns the substring from
     * {@code startOfTail} that is listed in {@code known}, preferring the full remainder or the
     * part before the next {@code _<digits>} when the full string is not listed.
     */
    private static String tailSegmentMatchingCustomField(String s, int startOfTail, Set<String> known) {
        String tail = s.substring(startOfTail);
        if (known.contains(tail)) {
            return tail;
        }
        Matcher next = NEXT_ROW_INDEX.matcher(tail);
        if (next.find() && next.start() > 0) {
            String beforeNextIndex = tail.substring(0, next.start());
            if (known.contains(beforeNextIndex)) {
                return beforeNextIndex;
            }
        }
        return null;
    }
}
