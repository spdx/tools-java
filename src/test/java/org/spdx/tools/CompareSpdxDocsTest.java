/**
 * Copyright (c) 2020 Source Auditor Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
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
 * SPDX-License-Identifier: Apache-2.0
 */
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.spdx.core.DefaultModelStore;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.ModelRegistry;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.SpdxModelInfoV2_X;
import org.spdx.library.model.v3_0_1.SpdxModelInfoV3_0;
import org.spdx.spreadsheetstore.SpreadsheetException;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tools.compare.DocumentSheet;
import org.spdx.tools.compare.MultiDocumentSpreadsheet;

import junit.framework.TestCase;

/**
 * @author Gary O'Neall
 * 
 * Test cases for CompareSpdxDocs
 *
 */
public class CompareSpdxDocsTest extends TestCase {

	static final String TEST_DIR = "testResources";
	static final String TEST_JSON_FILE_PATH_22 = TEST_DIR + File.separator + "SPDXJSONExample-v2.2.spdx.json";
	static final String TEST_RDF_FILE_PATH_22 = TEST_DIR + File.separator + "SPDXRdfExample-v2.2.spdx.rdf";
	static final String TEST_SPREADSHEET_XLS_FILE_PATH_22 = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.2.xls";
	static final String TEST_SPREADSHEET_XLSX_FILE_PATH_22 = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.2.xlsx";
	static final String TEST_TAG_FILE_PATH_22 = TEST_DIR + File.separator + "SPDXTagExample-v2.2.spdx";
	static final String TEST_XML_FILE_PATH_22 = TEST_DIR + File.separator + "SPDXXMLExample-v2.2.spdx.xml";
	static final String TEST_YAML_FILE_PATH_22 = TEST_DIR + File.separator + "SPDXYAMLExample-2.2.spdx.yaml";
	
	static final String TEST_JSON_FILE_PATH_23 = TEST_DIR + File.separator + "SPDXJSONExample-v2.3.spdx.json";
	static final String TEST_RDF_FILE_PATH_23 = TEST_DIR + File.separator + "SPDXRdfExample-v2.3.spdx.rdf";
	static final String TEST_SPREADSHEET_XLS_FILE_PATH_23 = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.3.xls";
	static final String TEST_SPREADSHEET_XLSX_FILE_PATH_23 = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.3.xlsx";
	static final String TEST_TAG_FILE_PATH_23 = TEST_DIR + File.separator + "SPDXTagExample-v2.3.spdx";
	static final String TEST_XML_FILE_PATH_23 = TEST_DIR + File.separator + "SPDXXMLExample-v2.3.spdx.xml";
	static final String TEST_YAML_FILE_PATH_23 = TEST_DIR + File.separator + "SPDXYAMLExample-2.3.spdx.yaml";

	static final String TEST_DIFF_FILE_COMMNENT_FILE_PATH = TEST_DIR + File.separator + "DifferentFileComment.spdx.yaml";
	


	Path tempDirPath;
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV3_0());
		ModelRegistry.getModelRegistry().registerModel(new SpdxModelInfoV2_X());
		DefaultModelStore.initialize(new InMemSpdxStore(), "http://default/namespace", new ModelCopyManager());
		tempDirPath = Files.createTempDirectory("spdx-tools-test-");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		SpdxConverterTestV2.deleteDirAndFiles(tempDirPath);
	}
	
	public void testCompareDocumentsv23() throws OnlineToolException, InvalidSPDXAnalysisException, IOException, InvalidFileNameException {
		String outputFilePath = tempDirPath + File.separator + "comp.xlsx";
		String[] params = new String[] {outputFilePath, 
				TEST_JSON_FILE_PATH_23, 
				TEST_RDF_FILE_PATH_23,
				TEST_SPREADSHEET_XLS_FILE_PATH_23,
				TEST_SPREADSHEET_XLSX_FILE_PATH_23,
				TEST_TAG_FILE_PATH_23,
				TEST_XML_FILE_PATH_23, 
				TEST_YAML_FILE_PATH_23
		};
		CompareSpdxDocs.onlineFunction(params);
		MultiDocumentSpreadsheet result = new MultiDocumentSpreadsheet(new File(outputFilePath), false, true);
		DocumentSheet docSheet = result.getDocumentSheet();
		Row resultRow = docSheet.getSheet().getRow(docSheet.getFirstDataRow());
		Cell cell = resultRow.getCell(1);
		int nextCol = 2;
		while (Objects.nonNull(cell) && !cell.getStringCellValue().isEmpty()) {
			assertTrue("Equals".equals(cell.getStringCellValue()) || "N/A".equals(cell.getStringCellValue()));
			cell = resultRow.getCell(nextCol++);
		}
	}
	
	public void testCompareDocumentsv22() throws OnlineToolException, SpreadsheetException {
		String outputFilePath = tempDirPath + File.separator + "comp.xlsx";
		String[] params = new String[] {outputFilePath, TEST_JSON_FILE_PATH_22, 
				TEST_RDF_FILE_PATH_22,	TEST_SPREADSHEET_XLS_FILE_PATH_22, 
				TEST_SPREADSHEET_XLSX_FILE_PATH_22, TEST_TAG_FILE_PATH_22,
				TEST_XML_FILE_PATH_22, 
				TEST_YAML_FILE_PATH_22
		};
		CompareSpdxDocs.onlineFunction(params);
		MultiDocumentSpreadsheet result = new MultiDocumentSpreadsheet(new File(outputFilePath), false, true);
		DocumentSheet docSheet = result.getDocumentSheet();
		Row resultRow = docSheet.getSheet().getRow(docSheet.getFirstDataRow());
		Cell cell = resultRow.getCell(1);
		int nextCol = 2;
		while (Objects.nonNull(cell) && !cell.getStringCellValue().isEmpty()) {
			String cellResult = cell.getStringCellValue();
			assertTrue("Equals".equals(cell.getStringCellValue()) || "N/A".equals(cellResult));
			cell = resultRow.getCell(nextCol++);
		}
	}
	
	
	public void testDifferentDocuments() throws OnlineToolException, SpreadsheetException {
		String outputFilePath = tempDirPath + File.separator + "comp.xlsx";
		String[] params = new String[] {outputFilePath, 
				TEST_YAML_FILE_PATH_22, TEST_DIFF_FILE_COMMNENT_FILE_PATH
		};
		CompareSpdxDocs.onlineFunction(params);
		MultiDocumentSpreadsheet result = new MultiDocumentSpreadsheet(new File(outputFilePath), false, true);
		DocumentSheet docSheet = result.getDocumentSheet();
		Row resultRow = docSheet.getSheet().getRow(docSheet.getFirstDataRow());
		Row titleRow = docSheet.getSheet().getRow(docSheet.getFirstDataRow()-1);
		Cell cell = resultRow.getCell(1);
		int nextCol = 2;
		while (Objects.nonNull(cell) && !cell.getStringCellValue().isEmpty()) {
			if ("Document Describes".equals(titleRow.getCell(nextCol-1).getStringCellValue()) || "Relationships".equals(titleRow.getCell(nextCol-1).getStringCellValue())) {
				assertTrue("Diff".equals(cell.getStringCellValue()));
			} else {
				assertTrue("Equals".equals(cell.getStringCellValue()) || "N/A".equals(cell.getStringCellValue()));
			}
			cell = resultRow.getCell(nextCol++);
		}
	}
}
