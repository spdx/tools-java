/**
 * Copyright (c) 2013 Source Auditor Inc.
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
*/
package org.spdx.tools.compare;

import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxFile;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Abstract worksheet for any comparison involving files.
 *
 * The first column is the file path, second column indicates if all documents are equal,
 * columns 3 through N are for the values of the individual documents
 * @author Gary O'Neall
 *
 */
public abstract class AbstractFileCompareSheet extends AbstractSheet {

	static final int FILENAME_COL_WIDTH = 80;
	static final int DIFF_COL_WIDTH = 10;
	static final int FILENAME_COL = 0;
	static final int DIFF_COL = 1;
	static final int FIRST_DOCUMENT_COL = 2;
	static final String FILENAME_TITLE = "File Path";
	static final String DIFF_TITLE = "Same/Diff";
	static final String DIFFERENT_VALUE = "Different";
	static final String EQUAL_VALUE = "Equal";
	static final String NO_FILE_VALUE = "[No File]";
	private static final int MAX_VALUE_LENGTH = 32000;

	private NormalizedFileNameComparator normalizedFileNameComparator = new NormalizedFileNameComparator();

	/**
	 * @param workbook
	 * @param sheetName
	 */
	public AbstractFileCompareSheet(Workbook workbook, String sheetName) {
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
	public static void create(Workbook wb, String sheetName, int columnWidth) {
		int sheetNum = wb.getSheetIndex(sheetName);
		if (sheetNum >= 0) {
			wb.removeSheetAt(sheetNum);
		}
		Sheet sheet = wb.createSheet(sheetName);
		Row row = sheet.createRow(0);
		CellStyle headerStyle = AbstractSheet.createHeaderStyle(wb);
		CellStyle defaultStyle = AbstractSheet.createLeftWrapStyle(wb);
		sheet.setColumnWidth(FILENAME_COL, FILENAME_COL_WIDTH*256);
		sheet.setDefaultColumnStyle(FILENAME_COL, defaultStyle);
		Cell fileNameHeadercell = row.createCell(FILENAME_COL);
		fileNameHeadercell.setCellStyle(headerStyle);
		fileNameHeadercell.setCellValue(FILENAME_TITLE);
		sheet.setColumnWidth(DIFF_COL, DIFF_COL_WIDTH*256);
		sheet.setDefaultColumnStyle(DIFF_COL, defaultStyle);
		Cell diffHeaderCell = row.createCell(DIFF_COL);
		diffHeaderCell.setCellStyle(headerStyle);
		diffHeaderCell.setCellValue(DIFF_TITLE);

		for (int i = FIRST_DOCUMENT_COL; i < MultiDocumentSpreadsheet.MAX_DOCUMENTS + FIRST_DOCUMENT_COL; i++) {
			sheet.setColumnWidth(i, columnWidth*256);
			sheet.setDefaultColumnStyle(i, defaultStyle);
			Cell cell = row.createCell(i);
			cell.setCellStyle(headerStyle);
		}
	}

	/**
	 * @param files Array of SPDX document files - arrays must be sorted
	 * @param docNames Document names.  Much match the documents in the files.
	 * @throws InvalidSPDXAnalysisException 
	 */
	public void importCompareResults(SpdxComparer comparer, List<List<SpdxFile>> files, List<String> docNames) throws SpdxCompareException, InvalidSPDXAnalysisException {
		if (docNames == null) {
			throw(new SpdxCompareException("Document names can not be null"));
		}
		if (files.size() != docNames.size()) {
			throw(new SpdxCompareException("Number of document names does not match the number of SPDX documents being compared"));
		}
		this.clear();
		Row header = sheet.getRow(0);
		for (int i = 0; i < docNames.size(); i++) {
			Cell headerCell = header.getCell(i + FIRST_DOCUMENT_COL);
			headerCell.setCellValue(docNames.get(i));
		}
		int[] fileIndexes = new int[files.size()];
		for (int i = 0; i < fileIndexes.length; i++) {
			fileIndexes[i] = 0;
		}
		while (!allFilesExhausted(files, fileIndexes)) {
			Row currentRow = this.addRow();
			Optional<String> fileName = getNextFileName(files, fileIndexes);
			Cell fileNameCell = currentRow.createCell(FILENAME_COL);
			fileNameCell.setCellValue(NormalizedFileNameComparator.normalizeFileName(fileName));
			boolean allValuesMatch = true;
			SpdxFile lastFile = null;
			int lastDocIndex = 0;
			// fill in the data cells and see if all values match
			for (int i = 0; i < files.size(); i++) {
				Cell cell = currentRow.createCell(i + FIRST_DOCUMENT_COL);
				if (fileIndexes[i] < files.get(i).size() &&
						normalizedFileNameComparator.compare(files.get(i).get(fileIndexes[i]).getName(), fileName) == 0) {
					String val = getFileValue(files.get(i).get(fileIndexes[i]));
					if (allValuesMatch && lastFile != null &&
							!valuesMatch(comparer, lastFile, lastDocIndex, files.get(i).get(fileIndexes[i]), i)) {
						allValuesMatch = false;
					}
					lastFile = files.get(i).get(fileIndexes[i]);
					if (val.length() > MAX_VALUE_LENGTH) {
						val = val.substring(0, MAX_VALUE_LENGTH-9) + "[more...]";
					}
					cell.setCellValue(val);
					fileIndexes[i]++;
				} else {
					cell.setCellValue(NO_FILE_VALUE);
					allValuesMatch = false;	// no file name, no match
				}
			}
			Cell diffCell = currentRow.createCell(DIFF_COL);
			if (allValuesMatch) {
				setCellAllEqual(diffCell);
			} else {
				setCellDifference(diffCell);
			}
		}
	}

	/**
	 * Returns true if the two values are equal
	 * @param fileA
	 * @param docIndexA
	 * @param fileB
	 * @param docIndexB
	 * @return
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException 
	 */
	abstract boolean valuesMatch(SpdxComparer comparer, SpdxFile fileA, int docIndexA,
			SpdxFile fileB, int docIndexB) throws SpdxCompareException, InvalidSPDXAnalysisException;

	/**
	 * @param cell
	 */
	private void setCellDifference(Cell cell) {
		cell.setCellValue(DIFFERENT_VALUE);
		cell.setCellStyle(yellowWrapped);
	}

	/**
	 * @param cell
	 */
	private void setCellAllEqual(Cell cell) {
		cell.setCellValue(EQUAL_VALUE);
		cell.setCellStyle(greenWrapped);
	}

	/**
	 * Get the string value to put in the value column
	 * @param spdxFile
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	abstract String getFileValue(SpdxFile spdxFile) throws InvalidSPDXAnalysisException;

	/**
	 * Get the next filename in alpha sort order from the files
	 * @param files
	 * @param fileIndexes
	 * @return
	 * @throws InvalidSPDXAnalysisException 
	 */
	private Optional<String> getNextFileName(List<List<SpdxFile>> files, int[] fileIndexes) throws InvalidSPDXAnalysisException {
		Optional<String> retval = Optional.empty();
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).size() > fileIndexes[i]) {
				Optional<String> fileName = files.get(i).get(fileIndexes[i]).getName();
				if (!retval.isPresent() || normalizedFileNameComparator.compare(retval, fileName) > 0) {
					retval = fileName;
				}
			}
		}
		return retval;
	}

	/**
	 * Returns true if the file indexes is greater than the size of the files for all documents
	 * @param files
	 * @param fileIndexes
	 * @return
	 */
	private boolean allFilesExhausted(List<List<SpdxFile>> files, int[] fileIndexes) {
		for (int i = 0; i < fileIndexes.length; i++) {
			if (fileIndexes[i] < files.get(i).size()) {
				return false;
			}
		}
		return true;
	}
}
