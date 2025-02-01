/**
 * SPDX-FileCopyrightText: Copyright (c) 2013 Source Auditor Inc.
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools.compare;

import java.util.Collection;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet with results for file AttributionText comparison results
 * @author Gary O'Neall
 */
public class FileContributorsSheet extends AbstractFileCompareSheet {

	private static final int FILE_CONTRIBUTOR_COL_WIDTH = 50;

	public FileContributorsSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	static void create(Workbook wb, String sheetName) {
		AbstractFileCompareSheet.create(wb, sheetName, FILE_CONTRIBUTOR_COL_WIDTH);
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#valuesMatch(org.spdx.compare.SpdxComparer, org.spdx.rdfparser.SpdxFile, int, org.spdx.rdfparser.SpdxFile, int)
	 */
	@Override
	boolean valuesMatch(SpdxComparer comparer, SpdxFile fileA, int docIndexA,
			SpdxFile fileB, int docIndexB) throws SpdxCompareException {
		return SpdxComparer.stringCollectionsEqual(fileA.getFileContributors(), fileB.getFileContributors());
	}

	/* (non-Javadoc)
	 * @see org.spdx.compare.AbstractFileCompareSheet#getFileValue(org.spdx.rdfparser.SpdxFile)
	 */
	@Override
	String getFileValue(SpdxFile spdxFile) {
		StringBuilder sb = new StringBuilder();
		Collection<String> contributors = spdxFile.getFileContributors();
		if (contributors != null && contributors.size() > 0) {
			Iterator<String> iter = contributors.iterator();
			sb.append(iter.next());
			while (iter.hasNext()) {
				sb.append(", ");
				sb.append(iter.next());
			}
		}
		return sb.toString();
	}

}
