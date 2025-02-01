/**
 * Copyright (c) 2013 Source Auditor Inc.
 * Copyright (c) 2013 Black Duck Software Inc.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.library.model.v2.SpdxFile;
import org.spdx.spreadsheetstore.SpreadsheetException;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

/**
 * Spreadsheet holding the results of a comparison from multiple SPDX documents
 * Each sheet contains the comparison result results with the columns representing the SPDX documents
 * and the rows representing the SPDX fields.
 *
 * The sheets include:
 *   - document: Document level fields Created, Data License, Document Comment, created date, creator comment
 *   - creator: Creators
 *   - package: Package level fields name, version, filename, supplier, ...
 *   - extracted license info: Extracted license text and identifiers
 *   - file checksums: file checksums
 *   - file concluded: license concluded for each file
 *   - file licenseInfo: license information from each file
 *   - file license comments: license comments from each file
 *   - file artifactOfs: artifact of for all files
 *   - file type: file type of all files
 *   - reviewers: review information
 *   - verification: List of any verification errors
 *
 * @author Gary O'Neall
 *
 */
public class MultiDocumentSpreadsheet {

	protected static final class SpdxFileComparator implements Comparator<SpdxFile> {

		private NormalizedFileNameComparator normalizedFileNameComparator = new NormalizedFileNameComparator();

		/* (non-Javadoc)
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		@Override
		public int compare(SpdxFile arg0, SpdxFile arg1) {
			try {
				return normalizedFileNameComparator.compare(arg0.getName(), arg1.getName());
			} catch (InvalidSPDXAnalysisException e) {
				logger.error("Error getting SPDX file names",e);
				throw new RuntimeException(e);
			}
		}
	}

	protected File saveFile;
	protected Workbook workbook;

	private boolean readonly;
	private SpdxFileComparator fileComparator = new SpdxFileComparator();

	static Logger logger = LoggerFactory.getLogger(MultiDocumentSpreadsheet.class);
	protected static final String DOCUMENT_SHEET_NAME = "Document";
	protected DocumentSheet documentSheet;
	protected static final String CREATOR_SHEET_NAME = "Creator";
	protected CreatorSheet creatorSheet;
	protected static final String PACKAGE_SHEET_NAME = "Package";
	protected PackageSheet packageSheet;
	protected static final String EXTRACTED_LICENSE_SHEET_NAME = "Extracted Licenses";
	protected ExtractedLicenseSheet extractedLicenseSheet;
	protected static final String FILE_CHECKSUM_SHEET_NAME = "File Checksum";
	protected FileChecksumSheet fileChecksumSheet;
	protected static final String FILE_CONCLUDED_SHEET_NAME = "File Concluded";
	protected FileConcludedSheet fileConcludedSheet;
	protected static final String FILE_FOUND_SHEET_NAME = "File Found Licenses";
	protected FileLicenseInfoSheet fileLicenseInfoSheet;
	protected static final String FILE_LICENSE_COMMENT_SHEET_NAME = "File License Comment";
	protected FileLicenseCommentsSheet fileLicenseCommentsSheet;
	protected static final String FILE_COMMENT_SHEET_NAME = "File Comment";
	protected FileCommentSheet fileCommentSheet;
	protected static final String FILE_TYPE_SHEET_NAME = "File Type";
	protected FileTypeSheet fileTypeSheet;
	protected static final String FILE_CONTRIBUTOR_SHEET_NAME = "File Contributors";
	protected FileContributorsSheet fileContributorsSheet;
	protected static final String FILE_ATTRIBUTION_SHEET_NAME = "File Attribution";
	protected FileAttributionSheet fileAttributionSheet;
	protected static final String FILE_NOTICE_SHEET_NAME = "File Notices";
	protected FileNoticeSheet fileNoticeSheet;
	protected static final String VERIFICATION_SHEET_NAME = "Verification Errors";
	public static final int MAX_DOCUMENTS = 25;
	protected static final String EXTERNAL_REFERENCES_SHEET_NAME = "Ext. Doc. References";
	protected static final String DOCUMENT_RELATIONSHIP_SHEET_NAME = "Doc. Relationships";
	protected static final String DOCUMENT_ANNOTATION_SHEET_NAME = "Doc. Annotations";
	protected static final String FILE_SPDX_ID_SHEET_NAME = "File IDs";
	protected static final String FILE_COPYRIGHT_SHEET_NAME = "File Copyrights";
	protected static final String FILE_ANNOTATION_SHEET_NAME = "File Annot.";
	protected static final String FILE_RELATIONSHIP_SHEET = "File Relationships";
	protected static final String SNIPPET_SHEET_NAME = "Snippets";

	protected VerificationSheet verificationSheet;

	protected ExternalReferencesSheet externalReferencesSheet;

	protected DocumentAnnotationSheet documentAnnotationSheet;

	protected DocumentRelationshipSheet documentRelationshipSheet;

	protected FileSpdxIdSheet fileSpdxIdSheet;

	protected FileCopyrightSheet fileCopyrightSheet;

	protected FileAnnotationSheet fileAnnotationSheet;

	protected FileRelationshipSheet fileRelationshipSheet;

	protected SnippetSheet snippetSheet;

	/**
	 * @param spreadsheetFile
	 * @param create
	 * @param readonly
	 * @throws SpreadsheetException
	 */
	public MultiDocumentSpreadsheet(File spreadsheetFile, boolean create,
			boolean readonly) throws SpreadsheetException {
		this.readonly = readonly;
		if (readonly && create) {
			throw(new SpreadsheetException("Can not create a readonly spreadsheet"));
		}
		if (!spreadsheetFile.exists()) {
			if (!create) {
				throw(new SpreadsheetException("File "+spreadsheetFile.getName()+" does not exist"));
			}
			try {
				create(spreadsheetFile);
			} catch (IOException ex) {
				logger.error("IO error creating spreadsheet: "+ex.getMessage());
				throw(new SpreadsheetException("I/O error creating spreadsheet"));
			}
		}
		this.saveFile = spreadsheetFile;
		InputStream input = null;
		try {
			input = new FileInputStream(spreadsheetFile);
			workbook = WorkbookFactory.create(input);
		} catch (FileNotFoundException ex) {
			logger.error("Can not open Excel file.  File "+
					spreadsheetFile.getName()+" does not exist");
			throw(new SpreadsheetException("Can not open Excel file.  File "+
					spreadsheetFile.getName()+" does not exist"));
		} catch (IOException ex) {
			logger.error("IO Exception opening excel workbook: "+ex.getMessage());
			throw(new SpreadsheetException("IO Exception opening excel workbook.  See log for more detail."));
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException ex) {
					logger.warn("IO Error closing excel file: "+ex.getMessage());
				}
			}
		}
		documentSheet = new DocumentSheet(this.workbook, DOCUMENT_SHEET_NAME);
		externalReferencesSheet = new ExternalReferencesSheet(this.workbook, EXTERNAL_REFERENCES_SHEET_NAME);
		creatorSheet = new CreatorSheet(this.workbook, CREATOR_SHEET_NAME);
		documentAnnotationSheet = new DocumentAnnotationSheet(this.workbook, DOCUMENT_ANNOTATION_SHEET_NAME);
		documentRelationshipSheet = new DocumentRelationshipSheet(this.workbook, DOCUMENT_RELATIONSHIP_SHEET_NAME);
		packageSheet = new PackageSheet(this.workbook, PACKAGE_SHEET_NAME);
		extractedLicenseSheet = new ExtractedLicenseSheet(this.workbook, EXTRACTED_LICENSE_SHEET_NAME);
		fileSpdxIdSheet = new FileSpdxIdSheet(this.workbook, FILE_SPDX_ID_SHEET_NAME);
		fileTypeSheet = new FileTypeSheet(this.workbook, FILE_TYPE_SHEET_NAME);
		fileChecksumSheet = new FileChecksumSheet(this.workbook, FILE_CHECKSUM_SHEET_NAME);
		fileConcludedSheet = new FileConcludedSheet(this.workbook, FILE_CONCLUDED_SHEET_NAME);
		fileLicenseInfoSheet = new FileLicenseInfoSheet(this.workbook, FILE_FOUND_SHEET_NAME);
		fileLicenseCommentsSheet = new FileLicenseCommentsSheet(this.workbook, FILE_LICENSE_COMMENT_SHEET_NAME);
		fileCopyrightSheet = new FileCopyrightSheet(this.workbook, FILE_COPYRIGHT_SHEET_NAME);
		fileCommentSheet = new FileCommentSheet(this.workbook, FILE_COMMENT_SHEET_NAME);
		fileNoticeSheet = new FileNoticeSheet(this.workbook, FILE_NOTICE_SHEET_NAME);
		fileContributorsSheet = new FileContributorsSheet(this.workbook, FILE_CONTRIBUTOR_SHEET_NAME);
		fileAttributionSheet = new FileAttributionSheet(this.workbook, FILE_ATTRIBUTION_SHEET_NAME);
		fileAnnotationSheet = new FileAnnotationSheet(this.workbook, FILE_ANNOTATION_SHEET_NAME);
		fileRelationshipSheet = new FileRelationshipSheet(this.workbook, FILE_RELATIONSHIP_SHEET);
		snippetSheet = new SnippetSheet(this.workbook, SNIPPET_SHEET_NAME);
		verificationSheet = new VerificationSheet(this.workbook, VERIFICATION_SHEET_NAME);
		String verify = this.verifyWorkbook();
		if (verify != null && !verify.isEmpty()) {
			logger.error("Invalid workbook: "+verify);
			throw(new SpreadsheetException("Invalid workbook: "+verify));
		}
	}

	/* (non-Javadoc)
	 * @see org.spdx.spdxspreadsheet.AbstractSpreadsheet#create(java.io.File)
	 */
	public void create(File spreadsheetFile) throws IOException, SpreadsheetException {
		if (!spreadsheetFile.createNewFile()) {
			logger.error("Unable to create "+spreadsheetFile.getName());
			throw(new SpreadsheetException("Unable to create "+spreadsheetFile.getName()));
		}
		FileOutputStream excelOut = null;
		try {
			excelOut = new FileOutputStream(spreadsheetFile);
			Workbook wb = new XSSFWorkbook();
			DocumentSheet.create(wb, DOCUMENT_SHEET_NAME);
			CreatorSheet.create(wb, CREATOR_SHEET_NAME);
			ExternalReferencesSheet.create(wb, EXTERNAL_REFERENCES_SHEET_NAME);
			DocumentAnnotationSheet.create(wb, DOCUMENT_ANNOTATION_SHEET_NAME);
			DocumentRelationshipSheet.create(wb, DOCUMENT_RELATIONSHIP_SHEET_NAME);
			PackageSheet.create(wb, PACKAGE_SHEET_NAME);
			ExtractedLicenseSheet.create(wb, EXTRACTED_LICENSE_SHEET_NAME);
			FileSpdxIdSheet.create(wb, FILE_SPDX_ID_SHEET_NAME);
			FileChecksumSheet.create(wb, FILE_CHECKSUM_SHEET_NAME);
			FileConcludedSheet.create(wb, FILE_CONCLUDED_SHEET_NAME);
			FileLicenseInfoSheet.create(wb, FILE_FOUND_SHEET_NAME);
			FileCommentSheet.create(wb, FILE_COMMENT_SHEET_NAME);
			FileCopyrightSheet.create(wb, FILE_COPYRIGHT_SHEET_NAME);
			FileLicenseCommentsSheet.create(wb, FILE_LICENSE_COMMENT_SHEET_NAME);
			FileTypeSheet.create(wb, FILE_TYPE_SHEET_NAME);
			FileContributorsSheet.create(wb, FILE_CONTRIBUTOR_SHEET_NAME);
			FileAttributionSheet.create(wb, FILE_ATTRIBUTION_SHEET_NAME);
			FileNoticeSheet.create(wb, FILE_NOTICE_SHEET_NAME);
			FileAnnotationSheet.create(wb, FILE_ANNOTATION_SHEET_NAME);
			FileRelationshipSheet.create(wb, FILE_RELATIONSHIP_SHEET);
			SnippetSheet.create(wb, SNIPPET_SHEET_NAME);
			VerificationSheet.create(wb, VERIFICATION_SHEET_NAME);
			wb.write(excelOut);
		} finally {
		    if(excelOut != null){
		        excelOut.close();
		    }
		}
	}

	public void importCompareResults(SpdxComparer comparer, List<String> docNames) throws SpdxCompareException, InvalidSPDXAnalysisException {
		if (docNames == null) {
			throw(new SpdxCompareException("Doc names can not be null"));
		}
		if (comparer == null) {
			throw(new SpdxCompareException("Comparer names can not be null"));
		}
		if (docNames.size() != comparer.getNumSpdxDocs()) {
			throw(new SpdxCompareException("Number of document names does not match the number of documents compared"));
		}

		List<List<SpdxFile>> files = new ArrayList<>();
		for (int i = 0; i < comparer.getNumSpdxDocs(); i++) {
			List<SpdxFile> docFiles = comparer.collectAllFiles(comparer.getSpdxDoc(i));
			Collections.sort(docFiles, fileComparator);
			files.add(docFiles);
		}
		documentSheet.importCompareResults(comparer, docNames);
		documentSheet.resizeRows();
		creatorSheet.importCompareResults(comparer, docNames);
		creatorSheet.resizeRows();
		externalReferencesSheet.importCompareResults(comparer, docNames);
		externalReferencesSheet.resizeRows();
		documentAnnotationSheet.importCompareResults(comparer, docNames);
		documentAnnotationSheet.resizeRows();
		documentRelationshipSheet.importCompareResults(comparer, docNames);
		documentRelationshipSheet.resizeRows();
		packageSheet.importCompareResults(comparer, docNames);
		packageSheet.resizeRows();
		fileSpdxIdSheet.importCompareResults(comparer, files, docNames);
		fileSpdxIdSheet.resizeRows();
		extractedLicenseSheet.importCompareResults(comparer, docNames);
		extractedLicenseSheet.resizeRows();
		fileChecksumSheet.importCompareResults(comparer, files, docNames);
		fileChecksumSheet.resizeRows();
		fileConcludedSheet.importCompareResults(comparer, files, docNames);
		fileConcludedSheet.resizeRows();
		fileLicenseInfoSheet.importCompareResults(comparer, files, docNames);
		fileLicenseInfoSheet.resizeRows();
		fileCopyrightSheet.importCompareResults(comparer, files, docNames);
		fileCopyrightSheet.resizeRows();
		fileCommentSheet.importCompareResults(comparer, files, docNames);
		fileCommentSheet.resizeRows();
		fileLicenseCommentsSheet.importCompareResults(comparer, files, docNames);
		fileLicenseCommentsSheet.resizeRows();
		fileTypeSheet.importCompareResults(comparer, files, docNames);
		fileTypeSheet.resizeRows();
		fileContributorsSheet.importCompareResults(comparer, files, docNames);
		fileContributorsSheet.resizeRows();
		fileAttributionSheet.importCompareResults(comparer, files, docNames);
		fileAttributionSheet.resizeRows();
		fileNoticeSheet.importCompareResults(comparer, files, docNames);
		fileNoticeSheet.resizeRows();
		fileAnnotationSheet.importCompareResults(comparer, files, docNames);
		fileAnnotationSheet.resizeRows();
		fileRelationshipSheet.importCompareResults(comparer, files, docNames);
		fileRelationshipSheet.resizeRows();
		snippetSheet.importCompareResults(comparer, docNames);
		snippetSheet.resizeRows();
	}

	public void clear() {
		documentSheet.clear();
		creatorSheet.clear();
		packageSheet.clear();
		extractedLicenseSheet.clear();
		fileChecksumSheet.clear();
		fileConcludedSheet.clear();
		fileLicenseInfoSheet.clear();
		fileLicenseCommentsSheet.clear();
		verificationSheet.clear();
		fileCommentSheet.clear();
		fileContributorsSheet.clear();
		fileAttributionSheet.clear();
		fileNoticeSheet.clear();
		snippetSheet.clear();
	}

	public String verifyWorkbook() {
		StringBuilder sb = new StringBuilder();
		String sheetVerify = documentSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append(sheetVerify);
		}
		sheetVerify = creatorSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = packageSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = extractedLicenseSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileChecksumSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileConcludedSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileLicenseInfoSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileCommentSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileLicenseCommentsSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileContributorsSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileAttributionSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = fileNoticeSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = verificationSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		sheetVerify = snippetSheet.verify();
		if (sheetVerify != null && !sheetVerify.isEmpty()) {
			sb.append("; ");
			sb.append(sheetVerify);
		}
		if (sb.length() > 0) {
			return sb.toString();
		} else {
			return null;
		}
	}

	/**
	 * @param verificationErrors
	 * @param docNames
	 * @throws SpreadsheetException
	 */
	public void importVerificationErrors(
			List<List<String>> verificationErrors, List<String> docNames) throws SpreadsheetException {
		this.verificationSheet.importVerificationErrors(verificationErrors, docNames);
		this.verificationSheet.resizeRows();
	}
	
	/**
	 * @throws SpreadsheetException
	 *
	 */
	public void close() throws SpreadsheetException {
		try {
			writeToFile(this.saveFile);
		} catch (IOException ex) {
			logger.error("Error writing excel sheet to file: "+ex.getMessage());
			throw(new SpreadsheetException("Error writing excel workbook to file, see log for details."));
		}
	}
	
	/**
	 * Writes the spreadsheet to a file
	 * @throws IOException
	 */
	public void writeToFile(File file) throws IOException {
		if (readonly) {
			return;
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			this.workbook.write(out);
		} finally {
			if (out != null) {
				out.close();
			}
		}

	}

	public DocumentSheet getDocumentSheet() {
		return this.documentSheet;
	}

}
