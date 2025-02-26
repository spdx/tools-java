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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.license.AnyLicenseInfo;
import org.spdx.library.model.license.ExtractedLicenseInfo;
import org.spdx.utility.compare.LicenseCompareHelper;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Comparison results for extracted licenses
 * Column 1 contains the extracted text
 * Columns 2 through N contains the license information in the format licenseId [licenseName] {licenceUrls} (licenseComment)
 * @author Gary O'Neall
 */
public class ExtractedLicenseSheet extends AbstractSheet {

	private static final class ExtractedLicenseComparator implements Comparator<AnyLicenseInfo>, Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(AnyLicenseInfo o1, AnyLicenseInfo o2) {
			if (o1 instanceof ExtractedLicenseInfo) {
				if (o2 instanceof ExtractedLicenseInfo) {
					ExtractedLicenseInfo l1 = (ExtractedLicenseInfo)o1;
					ExtractedLicenseInfo l2 = (ExtractedLicenseInfo)o2;
					try {
						String extracted1 = l1.getExtractedText();
						String extracted2 = l2.getExtractedText();
						if (extracted1 == null && extracted2 == null) {
							return 0;
						} else if (extracted1 == null) {
							return -1;
						} else if (extracted2 == null) {
							return 1;
						} else {
							return extracted1.compareTo(extracted2);
						}
					} catch(InvalidSPDXAnalysisException ex) {
						logger.error("Error comparing extracted license text",ex);
						throw new RuntimeException(ex);
					}
				} else {
					return 1;
				}
			} else {
				return -1;
			}
		}
	}

	ExtractedLicenseComparator extractedLicenseComparator = new ExtractedLicenseComparator();

	private static final int EXTRACTED_TEXT_COL = 0;
	private static final int EXTRACTED_TEXT_WIDTH = 100;
	private static final String EXTRACTED_TEXT_TITLE = "Extracted License Text";
	private static final int LIC_ID_COL_WIDTH = 20;
	private static final int FIRST_LIC_ID_COL = 1;

	/**
	 * @param workbook
	 * @param sheetName
	 */
	public ExtractedLicenseSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	/* (non-Javadoc)
	 * @see org.spdx.spdxspreadsheet.AbstractSheet#verify()
	 */
	@Override
	public String verify() {
		// Nothing to verify
		return null;
	}

	/**
	 * @param wb
	 * @param sheetName
	 */
	public static void create(Workbook wb, String sheetName) {
		int sheetNum = wb.getSheetIndex(sheetName);
		if (sheetNum >= 0) {
			wb.removeSheetAt(sheetNum);
		}
		Sheet sheet = wb.createSheet(sheetName);
		CellStyle headerStyle = AbstractSheet.createHeaderStyle(wb);
		CellStyle defaultStyle = AbstractSheet.createLeftWrapStyle(wb);
		Row row = sheet.createRow(0);
		sheet.setColumnWidth(EXTRACTED_TEXT_COL, EXTRACTED_TEXT_WIDTH*256);
		sheet.setDefaultColumnStyle(EXTRACTED_TEXT_COL, defaultStyle);
		Cell extractedHeaderCell = row.createCell(EXTRACTED_TEXT_COL);
		extractedHeaderCell.setCellStyle(headerStyle);
		extractedHeaderCell.setCellValue(EXTRACTED_TEXT_TITLE);
		for (int i = FIRST_LIC_ID_COL; i < MultiDocumentSpreadsheet.MAX_DOCUMENTS; i++) {
			sheet.setColumnWidth(i, LIC_ID_COL_WIDTH*256);
			sheet.setDefaultColumnStyle(i, defaultStyle);
			Cell cell = row.createCell(i);
			cell.setCellStyle(headerStyle);
		}
	}

	/**
	 * @param comparer
	 * @param docNames
	 * @throws InvalidSPDXAnalysisException
	 */
	public void importCompareResults(SpdxComparer comparer, List<String> docNames) throws SpdxCompareException, InvalidSPDXAnalysisException {
		if (comparer.getNumSpdxDocs() != docNames.size()) {
			throw(new SpdxCompareException("Number of document names does not match the number of SPDX documents"));
		}
		this.clear();
		Row header = sheet.getRow(0);
		int[] licenseIndexes = new int[comparer.getNumSpdxDocs()];
		AnyLicenseInfo[][] extractedLicenses = new AnyLicenseInfo[comparer.getNumSpdxDocs()][];
		for (int i = 0; i < extractedLicenses.length; i++) {
			Cell headerCell = header.getCell(FIRST_LIC_ID_COL+i);
			headerCell.setCellValue(docNames.get(i));
			AnyLicenseInfo[] docExtractedLicenses = comparer.getSpdxDoc(i).getExtractedLicenseInfos().toArray(new AnyLicenseInfo[comparer.getSpdxDoc(i).getExtractedLicenseInfos().size()]);
			Arrays.sort(docExtractedLicenses, extractedLicenseComparator);
			extractedLicenses[i] = docExtractedLicenses;
			licenseIndexes[i] = 0;
		}
		while (!allLicensesExhausted(extractedLicenses, licenseIndexes)) {
			Row currentRow = this.addRow();
			String extractedLicenseText = getNextExtractedLicenseText(extractedLicenses, licenseIndexes);
			Cell licenseTextCell = currentRow.createCell(EXTRACTED_TEXT_COL);
			licenseTextCell.setCellValue(extractedLicenseText);
			for (int i = 0; i < extractedLicenses.length; i++) {
				if (extractedLicenses[i].length > licenseIndexes[i]) {
					if  (extractedLicenses[i][licenseIndexes[i]] instanceof ExtractedLicenseInfo) {
						String compareExtractedText = ((ExtractedLicenseInfo)extractedLicenses[i][licenseIndexes[i]]).getExtractedText();
						if (LicenseCompareHelper.isLicenseTextEquivalent(extractedLicenseText,
								compareExtractedText)) {
							Cell licenseIdCell = currentRow.createCell(FIRST_LIC_ID_COL+i);
							licenseIdCell.setCellValue(formatLicenseInfo((ExtractedLicenseInfo)extractedLicenses[i][licenseIndexes[i]]));
							licenseIndexes[i]++;
						}
					} else {
						licenseIndexes[i]++;	// skip any licenses which are not non-standard licenses
					}
				}
			}
		}
	}

	/**
	 * Formats the license information for the license ID cell
	 * @param license
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	private String formatLicenseInfo(ExtractedLicenseInfo license) throws InvalidSPDXAnalysisException {
		StringBuilder sb = new StringBuilder(license.getLicenseId());
		if (license.getName() != null && !license.getName().isEmpty()) {
			sb.append("[");
			sb.append(license.getName());
			sb.append("]");
		}
		if (license.getSeeAlso() != null && license.getSeeAlso().size() > 0) {
			sb.append("{");
			Iterator<String> iter = license.getSeeAlso().iterator();
			sb.append(iter.next());
			while (iter.hasNext()) {
				sb.append(", ");
				sb.append(iter.next());
			}
			sb.append("}");
		}
		if (license.getComment() != null && !license.getComment().isEmpty()) {
			sb.append("(");
			sb.append(license.getComment());
			sb.append(")");
		}
		return sb.toString();
	}

	/**
	 * Get the next extracted license text in alpha sort order from the SPDXLicenseInfos
	 * @param licenseInfos
	 * @param licenseIndexes
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	private String getNextExtractedLicenseText(AnyLicenseInfo[][] licenseInfos, int[] licenseIndexes) throws InvalidSPDXAnalysisException {
		String retval = null;
		for (int i = 0; i < licenseInfos.length; i++) {
			if (licenseInfos[i].length > licenseIndexes[i]) {
				AnyLicenseInfo licenseInfo = licenseInfos[i][licenseIndexes[i]];
				String extractedText = "";
				if (licenseInfo instanceof ExtractedLicenseInfo) {
					extractedText = ((ExtractedLicenseInfo)licenseInfo).getExtractedText();
					if (extractedText == null) {
						extractedText = "";
					}
				}
				if (retval == null || retval.compareTo(extractedText) > 0) {
					retval = extractedText;
				}
			}
		}
		return retval;
	}

	/**
	 * Returns true if the license indexes is greater than the size of the licenseInfos for all documents
	 * @param licenseInfos
	 * @param licenseIndexes
	 * @return
	 */
	private boolean allLicensesExhausted(AnyLicenseInfo[][] licenseInfos, int[] licenseIndexes) {
		for (int i = 0; i < licenseInfos.length; i++) {
			if (licenseIndexes[i] < licenseInfos[i].length) {
				return false;
			}
		}
		return true;
	}

}
