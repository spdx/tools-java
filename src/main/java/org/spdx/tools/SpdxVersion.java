/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.IOException;
import java.util.Comparator;
import java.util.Properties;

import org.spdx.library.ListedLicenses;
import org.spdx.library.SpdxModelFactory;

/**
 * Static helper methods for tools and library version information
 *
 * @author Hirumal Priyashan
 */
public class SpdxVersion {
	
	static class SpdxVersionComparer implements Comparator<String> {
		
		/**
		 * @param version version to normalize - may be an SPDX 2.X style or a 3.X SemVer style
		 * @return version normalized to SemVer
		 */
		private String normalizeVersion(String version) {
			if (version.startsWith("SPDX-")) {
				return version.substring("SPDX-".length());
			} else {
				return version;
			}
		}

		@Override
		public int compare(String versionA, String versionB) {
			return normalizeVersion(versionA).compareTo(normalizeVersion(versionB));
		}

		
	}

    /**
     * Getter for the current tool Version
     *
     * @return Tool version
     */
    public static String getCurrentToolVersion() {
        final Properties properties = new Properties();
        try {
            properties.load(SpdxVersion.class.getClassLoader().getResourceAsStream("project.properties"));
            return properties.getProperty("version");
        } catch (IOException e) {
            return "Unknown tool version";
        }
    }

    /**
     * Getter for the library specification version
     *
     * @return The library specification version
     */
    public static String getLibraryVersion() {
    	return SpdxModelFactory.IMPLEMENTATION_VERSION;
    }
    
    /**
     * @return the latest spec version supported
     */
    public static String getLatestSpecVersion() {
    	return SpdxModelFactory.getLatestSpecVersion();
    }

    /**
     * Getter for the license list version
     *
     * @return The license list version
     */
    public static String getLicenseListVersion() {
        return ListedLicenses.getListedLicenses().getLicenseListVersion();
    }
}
