/**
 * Copyright (c) 2020 Source Auditor Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import org.spdx.library.Version;
import org.spdx.library.model.license.ListedLicenses;

/**
 * Static helper methods for tools and library version information
 *
 * @author Hirumal Priyashan
 */
public class SpdxVersion {

    private static final String CURRENT_TOOL_VERSION = "1.1.6";

    /**
     * Getter for the current tool Version
     *
     * @return Tool version
     */
    public static String getCurrentToolVersion() {
        return CURRENT_TOOL_VERSION;
    }

    /**
     * Getter for the library specification version
     *
     * @return The library specification version
     */
    public static String getLibraryVersion() {
        return Version.CURRENT_SPDX_VERSION;
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
