/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
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

	public static final java.util.List<String> SUPPORTED_SPDX_VERSIONS =
			java.util.Collections.unmodifiableList(
					java.util.Arrays.asList(
							"1.0", "1.1", "1.2", "2.0", "2.1", "2.2", "2.3",
							"3.0", "3.0.1"
					));

	/**
	 * Check if a version string is a valid supported SPDX version.
	 * @param version
	 * @return true if valid
	 */
	public static boolean isValidVersion(String version) {
		if (version == null) {
			return false;
		}
		String norm = version.startsWith("SPDX-")
				? version.substring(5) : version;
		return SUPPORTED_SPDX_VERSIONS.contains(norm);
	}

	/**
	 * Compares two SPDX version strings.
	 * @param versionA
	 * @param versionB
	 * @return negative if versionA < versionB, zero if equal, positive if
	 *         versionA > versionB
	 */
	public static int compareVersions(String versionA, String versionB) {
		if (versionA == null || versionB == null) {
			return 0;
		}
		String normA = versionA.startsWith("SPDX-")
				? versionA.substring(5) : versionA;
		String normB = versionB.startsWith("SPDX-")
				? versionB.substring(5) : versionB;
		String[] partsA = normA.split("\\.");
		String[] partsB = normB.split("\\.");
		int length = Math.max(partsA.length, partsB.length);
		for (int i = 0; i < length; i++) {
			String partA = i < partsA.length ? partsA[i].trim() : "0";
			String partB = i < partsB.length ? partsB[i].trim() : "0";
			try {
				int a = Integer.parseInt(partA);
				int b = Integer.parseInt(partB);
				if (a != b) {
					return Integer.compare(a, b);
				}
			} catch (NumberFormatException e) {
				int comp = partA.compareTo(partB);
				if (comp != 0) {
					return comp;
				}
			}
		}
		return 0;
	}
}
