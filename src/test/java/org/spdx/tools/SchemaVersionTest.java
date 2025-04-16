package org.spdx.tools;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;

import org.apache.commons.io.IOUtils;

import junit.framework.TestCase;

public class SchemaVersionTest extends TestCase {

    private String VERSION_REGEX = "spdx-schema-v(\\d+\\.\\d+(\\.\\d+)?)\\.json";

    public void testLatestSpdxSchemaVersionIsUpToDate() throws IOException {
        // Step 1: Find the latest JSON schema file in the resources directory
        Path resourcesDir = Paths.get("resources");


        Optional<Path> latestSchemaFile = Files.list(resourcesDir)
                .filter(path -> path.getFileName().toString().matches(VERSION_REGEX))
                .max(Comparator.comparing(path -> parseVersion(path.getFileName().toString()),
                        versionComparator));

        assertTrue("No SPDX schema file found in resources directory.",
                latestSchemaFile.isPresent());

        Path schemaFilePath = latestSchemaFile.get();
        String fileName = schemaFilePath.getFileName().toString();
        String version = extractVersionNumber(fileName);

        // Step 2: Compare the content of the file with the content from the URL
        String localSchemaContent = Files.readString(schemaFilePath);
        String remoteSchemaUrl = "https://spdx.org/schema/" + version + "/spdx-json-schema.json";
        String remoteSchemaContent = IOUtils.toString(new URL(remoteSchemaUrl), "UTF-8");

        // Step 3: Assert if the contents match
        assertEquals("The local SPDX schema file does not match the remote schema content.",
                localSchemaContent.trim(), remoteSchemaContent.trim());
    }

    private String extractVersionNumber(String fileName) {
        return fileName.replaceAll(VERSION_REGEX, "$1");
    }

    private int[] parseVersion(String fileName) {
        String version = extractVersionNumber(fileName);
        String[] parts = version.split("\\.");
        int[] versionNumbers = new int[3]; // [major, minor, patch]
        for (int i = 0; i < parts.length; i++) {
            versionNumbers[i] = Integer.parseInt(parts[i]);
        }
        return versionNumbers;
    }

    private Comparator<int[]> versionComparator = (v1, v2) -> {
        for (int i = 0; i < 3; i++) {
            int comparison = Integer.compare(v1[i], v2[i]);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    };
}
