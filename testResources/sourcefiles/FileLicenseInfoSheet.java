/**
 * SPDX-FileCopyrightText: Copyright (c) 2013 Source Auditor Inc.
 * SPDX-FileCopyrightText: Copyright (c) 2013 Black Duck Software Inc.
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
package org.spdx.tools.compare;

import java.util.Collection;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxFile;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet of the comparison results for the file seen licenses
 * @author Gary O'Neall
 */
public class FileLicenseInfoSheet extends AbstractFileCompareSheet {

	private static final int LICENSE_COL_WIDTH = 60;

	/**
	 * @param workbook
	 * @param sheetName
	 */
	public FileLicenseInfoSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}
	static void create(Workbook wb, String sheetName) {
		AbstractFileCompareSheet.create(wb, sheetName, LICENSE_COL_WIDTH);
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#getFileValue(org.spdx.rdfparser.SpdxFile)
	 */
	@Override
	String getFileValue(SpdxFile spdxFile) throws InvalidSPDXAnalysisException {
		if (spdxFile.getLicenseInfoFromFiles() == null || spdxFile.getLicenseInfoFromFiles().size() == 0) {
			return "";
		}
		
		Iterator<AnyLicenseInfo> iter = spdxFile.getLicenseInfoFromFiles().iterator();
		StringBuilder sb = new StringBuilder(iter.next().toString());
		while (iter.hasNext()) {
			sb.append(", ");
			sb.append(iter.next().toString());
		}
		return sb.toString();
	}
	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#valuesMatch(org.spdx.rdfparser.SpdxFile, int, org.spdx.rdfparser.SpdxFile, int)
	 */
	@Override
	boolean valuesMatch(SpdxComparer comparer, SpdxFile fileA, int docIndexA, SpdxFile fileB,
			int docIndexB) throws SpdxCompareException, InvalidSPDXAnalysisException {
		Collection<AnyLicenseInfo> licenseInfosA = fileA.getLicenseInfoFromFiles();
		Collection<AnyLicenseInfo> licenseInfosB = fileB.getLicenseInfoFromFiles();
		if (licenseInfosA.size() != licenseInfosB.size()) {
			return false;
		}
		for (AnyLicenseInfo licA:licenseInfosA) {
			boolean found = false;
			for (AnyLicenseInfo licB:licenseInfosB) {
				if (comparer.compareLicense(docIndexA, licA, docIndexB, licB)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;
	}
}
