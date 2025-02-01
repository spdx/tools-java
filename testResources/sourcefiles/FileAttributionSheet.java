/**
 * SPDX-FileCopyrightText: Copyright (c) 2013 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.compare;

import java.util.Collection;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxFile;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet with results for file contributor comparison results
 * @author Gary O'Neall
 */
public class FileAttributionSheet extends AbstractFileCompareSheet {

	private static final int FILE_ATTRIBUTION_COL_WIDTH = 50;

	public FileAttributionSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	static void create(Workbook wb, String sheetName) {
		AbstractFileCompareSheet.create(wb, sheetName, FILE_ATTRIBUTION_COL_WIDTH);
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#valuesMatch(org.spdx.compare.SpdxComparer, org.spdx.rdfparser.SpdxFile, int, org.spdx.rdfparser.SpdxFile, int)
	 */
	@Override
	boolean valuesMatch(SpdxComparer comparer, SpdxFile fileA, int docIndexA,
			SpdxFile fileB, int docIndexB) throws SpdxCompareException, InvalidSPDXAnalysisException {
		return SpdxComparer.stringCollectionsEqual(fileA.getAttributionText(), fileB.getAttributionText());
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#getFileValue(org.spdx.rdfparser.SpdxFile)
	 */
	@Override
	String getFileValue(SpdxFile spdxFile) throws InvalidSPDXAnalysisException {
		StringBuilder sb = new StringBuilder();
		Collection<String> attribution = spdxFile.getAttributionText();
		if (attribution != null && attribution.size() > 0) {
			Iterator<String> iter = attribution.iterator();
			sb.append(iter.next());
			while (iter.hasNext()) {
				sb.append(", ");
				sb.append(iter.next());
			}
		}
		return sb.toString();
	}

}
