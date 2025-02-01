/**
 * SPDX-FileCopyrightText: Copyright (c) 2013 Source Auditor Inc.
 * SPDX-FileCopyrightText: Copyright (c) 2013 Black Duck Software Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.compare;

import java.util.Optional;

import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet of comparison results for file license comments
 * @author Gary O'Neall
 *
 */
public class FileLicenseCommentsSheet extends AbstractFileCompareSheet {

	private static final int FILE_COMMENT_COL_WIDTH = 60;


	/**
	 * @param workbook
	 * @param sheetName
	 */
	public FileLicenseCommentsSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	static void create(Workbook wb, String sheetName) {
		AbstractFileCompareSheet.create(wb, sheetName, FILE_COMMENT_COL_WIDTH);
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#getFileValue(org.spdx.rdfparser.SpdxFile)
	 */
	@Override
	String getFileValue(SpdxFile spdxFile) throws InvalidSPDXAnalysisException {
	    Optional<String> licenseComments = spdxFile.getLicenseComments();
		if (!licenseComments.isPresent()) {
			return "";
		} else {
			return licenseComments.get();
		}
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#valuesMatch(org.spdx.compare.SpdxComparer, org.spdx.rdfparser.SpdxFile, int, org.spdx.rdfparser.SpdxFile, int)
	 */
	@Override
	boolean valuesMatch(SpdxComparer comparer, SpdxFile fileA, int docIndexA,
			SpdxFile fileB, int docIndexB) throws SpdxCompareException, InvalidSPDXAnalysisException {
		return SpdxComparer.stringsEqual(fileA.getLicenseComments(), fileB.getLicenseComments());
	}

}
