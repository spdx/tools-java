/**
 * SPDX-FileCopyrightText: Copyright (c) 2015 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.compare;

import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxFile;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet comparing file annotations
 * @author Gary O'Neall
 */
public class FileAnnotationSheet extends AbstractFileCompareSheet {

	static final int ANNOTATION_COL_WIDTH = 80;

	public FileAnnotationSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	static void create(Workbook wb, String sheetName) {
		AbstractFileCompareSheet.create(wb, sheetName, ANNOTATION_COL_WIDTH);
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#valuesMatch(org.spdx.compare.SpdxComparer, org.spdx.rdfparser.model.SpdxFile, int, org.spdx.rdfparser.model.SpdxFile, int)
	 */
	@Override
	boolean valuesMatch(SpdxComparer comparer, SpdxFile fileA, int docIndexA,
			SpdxFile fileB, int docIndexB) throws SpdxCompareException, InvalidSPDXAnalysisException {
		return CompareHelper.equivalent(fileA.getAnnotations(), fileB.getAnnotations());
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#getFileValue(org.spdx.rdfparser.model.SpdxFile)
	 */
	@Override
	String getFileValue(SpdxFile spdxFile) throws InvalidSPDXAnalysisException {
		return CompareHelper.annotationsToString(spdxFile.getAnnotations());
	}

}
