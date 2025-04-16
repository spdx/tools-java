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

import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.SpdxCreatorInformation;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Sheet to hold compare information at the document level:
 * 		Created, Data License, Document Comment
 * The first row summarizes which fields are different, the subsequent rows are the
 * specific date from each result
 * @author Gary O'Neall
 */
public class DocumentSheet extends AbstractSheet {

	int NUM_COLS = 13;
	int DOCUMENT_PATH_COL = 0;
	int DOCUMENT_NAME_COL = DOCUMENT_PATH_COL + 1;
	int SPDX_VERSION_COL = DOCUMENT_NAME_COL + 1;
	int DATA_LICENSE_COL = SPDX_VERSION_COL + 1;
	int SPDX_IDENTIFIER_COL = DATA_LICENSE_COL + 1;
	int DOCUMENT_NAMESPACE_COL = SPDX_IDENTIFIER_COL + 1;
	int SPDX_DOCUMENT_CONTENT_COL = DOCUMENT_NAMESPACE_COL + 1;
	int DOCUMENT_COMMENT_COL = SPDX_DOCUMENT_CONTENT_COL + 1;
	int CREATION_DATE_COL = DOCUMENT_COMMENT_COL + 1;
	int CREATOR_COMMENT_COL = CREATION_DATE_COL + 1;
	int LICENSE_LIST_VERSION_COL = CREATOR_COMMENT_COL + 1;
	int ANNOTATION_COL = LICENSE_LIST_VERSION_COL + 1;
	int RELATIONSHIP_COL = ANNOTATION_COL + 1;


	static final boolean[] REQUIRED = new boolean[] {true, true, true, true,
		true, true, true, false, true, false, false, false, false};
	static final String[] HEADER_TITLES = new String[] {"Document", "Document Name", "SPDX Version",
		"Data License", "ID", "Document Namespace", "Document Describes",
		"Document Comment", "Creation Date", "Creator Comment", "Lic. List. Ver.",
		"Annotations", "Relationships"};

	static final int[] COLUMN_WIDTHS = new int[] {30, 30, 15, 15, 15, 60, 40, 60,
		22, 60, 22, 80, 80};
	private static final String DIFFERENT_STRING = "Diff";
	private static final String EQUAL_STRING = "Equals";


	/**
	 * @param workbook
	 * @param sheetName
	 */
	public DocumentSheet(Workbook workbook, String sheetName) {
		super(workbook, sheetName);
	}

	/* (non-Javadoc)
	 * @see org.spdx.spdxspreadsheet.AbstractSheet#verify()
	 */
	@Override
	public String verify() {
		try {
			if (sheet == null) {
				return "Worksheet for SPDX Package Info does not exist";
			}
			Row firstRow = sheet.getRow(firstRowNum);
			for (int i = 0; i < NUM_COLS; i++) {
				Cell cell = firstRow.getCell(i+firstCellNum);
				if (cell == null ||
						cell.getStringCellValue() == null ||
						!cell.getStringCellValue().equals(HEADER_TITLES[i])) {
					return "Column "+HEADER_TITLES[i]+" missing for SPDX Package Info worksheet";
				}
			}
			return null;
		} catch (Exception ex) {
			return "Error in verifying SPDX Package Info work sheet: "+ex.getMessage();
		}
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
		for (int i = 0; i < HEADER_TITLES.length; i++) {
			sheet.setColumnWidth(i, COLUMN_WIDTHS[i]*256);
			sheet.setDefaultColumnStyle(i, defaultStyle);
			Cell cell = row.createCell(i);
			cell.setCellStyle(headerStyle);
			cell.setCellValue(HEADER_TITLES[i]);
		}
	}

	/**
	 * Import compare results from a comparison
	 * @param comparer Comparer which compared the documents
	 * @param docNames Document names - order must be the same as the documents provided
	 * @throws InvalidSPDXAnalysisException
	 */
	public void importCompareResults(SpdxComparer comparer, List<String> docNames)  throws SpdxCompareException, InvalidSPDXAnalysisException {
		if (comparer.getNumSpdxDocs() != docNames.size()) {
			throw(new SpdxCompareException("Number of document names does not match the number of SPDX documents"));
		}
		this.clear();
		addRow().createCell(DOCUMENT_PATH_COL).setCellValue("Compare Results");
		// create the rows
		for (int i = 0; i < docNames.size(); i++) {
			addRow().createCell(DOCUMENT_PATH_COL).setCellValue(docNames.get(i));
		}
		importDocumentNames(comparer);
		importSpdxVersion(comparer);
		importDataLicense(comparer);
		importSpdxId(comparer);
		importDocumentNamespace(comparer);
		importDocumentDescribes(comparer);
		importDocumentComments(comparer);
		importCreationDate(comparer);
		importCreatorComment(comparer);
		importLicenseListVersions(comparer);
		importAnnotations(comparer);
		importRelationships(comparer);
	}

    /**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException 
	 */
	private void importAnnotations(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(ANNOTATION_COL);
		if (comparer.isDocumentAnnotationsEquals()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(ANNOTATION_COL);
			cell.setCellValue(CompareHelper.annotationsToString((comparer.getSpdxDoc(i).getAnnotations())));
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException 
	 */
	private void importRelationships(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(RELATIONSHIP_COL);
		if (comparer.isDocumentRelationshipsEquals()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(RELATIONSHIP_COL);
			cell.setCellValue(CompareHelper.relationshipsToString(comparer.getSpdxDoc(i).getRelationships()));
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException
	 */
	private void importDocumentDescribes(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(SPDX_DOCUMENT_CONTENT_COL);
		if (comparer.isDocumentContentsEquals()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(SPDX_DOCUMENT_CONTENT_COL);
			cell.setCellValue(CompareHelper.formatSpdxElementList(comparer.getSpdxDoc(i).getDocumentDescribes()));
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException
	 */
	private void importDocumentNamespace(SpdxComparer comparer) throws InvalidSPDXAnalysisException, SpdxCompareException {
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(DOCUMENT_NAMESPACE_COL);
		cell.setCellValue("N/A");
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(DOCUMENT_NAMESPACE_COL);
			if (comparer.getSpdxDoc(i).getDocumentUri() != null) {
				cell.setCellValue(comparer.getSpdxDoc(i).getDocumentUri());
			}
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 */
	private void importSpdxId(SpdxComparer comparer) throws SpdxCompareException {
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(SPDX_IDENTIFIER_COL);
		cell.setCellValue("N/A");
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(SPDX_IDENTIFIER_COL);
			if (comparer.getSpdxDoc(i).getId() != null) {
				cell.setCellValue(comparer.getSpdxDoc(i).getId());
			}
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException
	 */
	private void importLicenseListVersions(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(LICENSE_LIST_VERSION_COL);
		if (comparer.isLicenseListVersionEqual()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(LICENSE_LIST_VERSION_COL);
			SpdxCreatorInformation creationInfo = comparer.getSpdxDoc(i).getCreationInfo();
			if (creationInfo != null) {
				Optional<String> licenseListVersion = creationInfo.getLicenseListVersion();
				if (licenseListVersion.isPresent()) {
					cell.setCellValue(licenseListVersion.get());
				}
			}
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException
	 */
	private void importSpdxVersion(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		// comparison row
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(SPDX_VERSION_COL);
		if (comparer.isSpdxVersionEqual()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(SPDX_VERSION_COL);
			if (comparer.getSpdxDoc(i).getSpecVersion() != null) {
				cell.setCellValue(comparer.getSpdxDoc(i).getSpecVersion());
			}
		}
	}

	/**
	 * @param comparer
	 */
	private void importDataLicense(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		// comparison row
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(DATA_LICENSE_COL);
		if (comparer.isDataLicenseEqual()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(DATA_LICENSE_COL);
			if (comparer.getSpdxDoc(i).getDataLicense() != null) {
				cell.setCellValue(comparer.getSpdxDoc(i).getDataLicense().toString());
			}
		}
	}

	/**
	 * @param comparer
	 */
	private void importDocumentComments(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		// comparison row
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(DOCUMENT_COMMENT_COL);
		if (comparer.isDocumentCommentsEqual()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(DOCUMENT_COMMENT_COL);
			Optional<String> comment = comparer.getSpdxDoc(i).getComment();
			if (comment.isPresent()) {
				cell.setCellValue(comment.get());
			}
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException
	 */
	private void importCreatorComment(SpdxComparer comparer) throws InvalidSPDXAnalysisException, SpdxCompareException {
		// comparison row
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(CREATOR_COMMENT_COL);
		if (comparer.isCreatorCommentsEqual()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(CREATOR_COMMENT_COL);
			SpdxCreatorInformation creationInfo = comparer.getSpdxDoc(i).getCreationInfo();
			if (creationInfo != null) {
				Optional<String> creatorComment = creationInfo.getComment();
				if (creatorComment.isPresent()) {
					cell.setCellValue(creatorComment.get());
				}
			}
		}
	}

	/**
	 * @param comparer
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException
	 */
	private void importCreationDate(SpdxComparer comparer) throws InvalidSPDXAnalysisException, SpdxCompareException {
		// comparison row
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(CREATION_DATE_COL);
		if (comparer.isCreatorDatesEqual()) {
			setCellEqualValue(cell);
		} else {
			setCellDifferentValue(cell);
		}
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(CREATION_DATE_COL);
			SpdxCreatorInformation creationInfo = comparer.getSpdxDoc(i).getCreationInfo();
			if (creationInfo != null) {
				cell.setCellValue(creationInfo.getCreated());
			}
		}
	}

	/**
	 * @param cell
	 */
	private void setCellDifferentValue(Cell cell) {
		cell.setCellValue(DIFFERENT_STRING);
		cell.setCellStyle(yellowWrapped);
	}

	/**
	 * @param cell
	 */
	private void setCellEqualValue(Cell cell) {
		cell.setCellValue(EQUAL_STRING);
		cell.setCellStyle(greenWrapped);
	}

	/**
	 * @throws SpdxCompareException
	 * @throws InvalidSPDXAnalysisException 
	 */
	private void importDocumentNames(SpdxComparer comparer) throws SpdxCompareException, InvalidSPDXAnalysisException {
		// comparison row
		Cell cell = sheet.getRow(getFirstDataRow()).createCell(DOCUMENT_NAME_COL);
		cell.setCellValue("N/A");
		// data rows
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			cell = sheet.getRow(getFirstDataRow()+i+1).createCell(DOCUMENT_NAME_COL);
			Optional<String> name = comparer.getSpdxDoc(i).getName();
			if (name.isPresent()) {
				cell.setCellValue(name.get());
			}
		}
	}

}
