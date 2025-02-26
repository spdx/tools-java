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

import java.util.Arrays;

import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.library.model.v2.enumerations.FileType;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet containing File Type
 * @author Gary O'Neall
 */
public class FileTypeSheet extends AbstractFileCompareSheet {

	private static final int FILE_TYPE_COL_WIDTH = 20;

	/**
	 * @param workbook
	 * @param sheetName
	 */
	public FileTypeSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	static void create(Workbook wb, String sheetName) {
		AbstractFileCompareSheet.create(wb, sheetName, FILE_TYPE_COL_WIDTH);
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#getFileValue(org.spdx.rdfparser.SpdxFile)
	 */
	@Override
	String getFileValue(SpdxFile spdxFile) throws InvalidSPDXAnalysisException {
		if (spdxFile.getFileTypes() == null ||spdxFile.getFileTypes().size() == 0) {
			return "";
		} else {
			FileType[] fileTypes = spdxFile.getFileTypes().toArray(new FileType[spdxFile.getFileTypes().size()]);
			String[] sFileTypes = new String[fileTypes.length];
			for (int i = 0; i < fileTypes.length; i++) {
				sFileTypes[i] = fileTypes[i].toString();
			}
			Arrays.sort(sFileTypes);
			StringBuilder sb = new StringBuilder(sFileTypes[0]);
			for (int i = 1; i < sFileTypes.length; i++) {
				sb.append(", ");
				sb.append(sFileTypes[i]);
			}
			return sb.toString();
		}
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#valuesMatch(org.spdx.compare.SpdxComparer, org.spdx.rdfparser.SpdxFile, int, org.spdx.rdfparser.SpdxFile, int)
	 */
	@Override
	boolean valuesMatch(SpdxComparer comparer, SpdxFile fileA, int docIndexA,
			SpdxFile fileB, int docIndexB) throws SpdxCompareException, InvalidSPDXAnalysisException {
		return SpdxComparer.stringsEqual(getFileValue(fileA), getFileValue(fileB));
	}

}
