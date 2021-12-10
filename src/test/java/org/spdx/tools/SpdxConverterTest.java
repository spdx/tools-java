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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.SpdxConstants;
import org.spdx.library.model.Relationship;
import org.spdx.library.model.SpdxDocument;
import org.spdx.library.model.SpdxFile;
import org.spdx.library.model.SpdxModelFactory;
import org.spdx.library.model.SpdxPackage;
import org.spdx.tools.SpdxToolsHelper.SerFileType;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

import junit.framework.TestCase;

/**
 * @author gary
 *
 */
public class SpdxConverterTest extends TestCase {
	
	static final String TEST_DIR = "testResources";
	static final String TEST_JSON_FILE_PATH = TEST_DIR + File.separator + "SPDXJSONExample-v2.2.spdx.json";
	static final String TEST_RDF_FILE_PATH = TEST_DIR + File.separator + "SPDXRdfExample-v2.2.spdx.rdf";
	static final String TEST_SPREADSHEET_XLS_FILE_PATH = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.2.xls";
	static final String TEST_SPREADSHEET_XLSX_FILE_PATH = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.2.xlsx";
	static final String TEST_TAG_FILE_PATH = TEST_DIR + File.separator + "SPDXTagExample-v2.2.spdx";
	static final String TEST_XML_FILE_PATH = TEST_DIR + File.separator + "SPDXXMLExample-v2.2.spdx.xml";
	static final String TEST_YAML_FILE_PATH = TEST_DIR + File.separator + "SPDXYAMLExample-2.2.spdx.yaml";

	Path tempDirPath;
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		tempDirPath = Files.createTempDirectory("spdx-tools-test-");
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		super.tearDown();
		deleteDirAndFiles(tempDirPath);
	}
	
	public static void deleteDirAndFiles(Path dirOrFile) {
		if (Objects.isNull(dirOrFile)) {
			return;
		}
		if (!Files.exists(dirOrFile, LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		if (Files.isDirectory(dirOrFile, LinkOption.NOFOLLOW_LINKS)) {
			try (DirectoryStream<Path> files = Files.newDirectoryStream(dirOrFile)) {
				for (Path file : files) {
					deleteDirAndFiles(file);
			      }
			} catch (IOException e) {
				System.err.println("IO error deleting directory or file "+e.getMessage());
			}
		}
		try {
			Files.delete(dirOrFile);
		} catch (IOException e) {
			System.err.println("IO error deleting directory or file "+e.getMessage());
		}
	}
	
	// Supported file types: JSON, XLS, XLSX, TAG, RDFXML, YAML or XML
	
	public void testXlsxToRDFXML() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String outFileName = "result.rdf.xml";
		Path outFilePath = tempDirPath.resolve(outFileName);
		SpdxConverter.convert(TEST_SPREADSHEET_XLSX_FILE_PATH, outFilePath.toString(), SerFileType.XLSX, SerFileType.RDFXML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_SPREADSHEET_XLSX_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.XLSX);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_SPREADSHEET_XLSX_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testTagToRDFXML() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String outFileName = "result.rdf.xml";
		Path outFilePath = tempDirPath.resolve(outFileName);
		SpdxConverter.convert(TEST_TAG_FILE_PATH, outFilePath.toString(), SerFileType.TAG, SerFileType.RDFXML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_TAG_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.TAG);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_TAG_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testYamlToRDFXML() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String outFileName = "result.rdf.xml";
		Path outFilePath = tempDirPath.resolve(outFileName);
		SpdxConverter.convert(TEST_YAML_FILE_PATH, outFilePath.toString(), SerFileType.YAML, SerFileType.RDFXML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_YAML_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.YAML);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_YAML_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testXmlToRDFXML() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String outFileName = "result.rdf.xml";
		Path outFilePath = tempDirPath.resolve(outFileName);
		SpdxConverter.convert(TEST_XML_FILE_PATH, outFilePath.toString(), SerFileType.XML, SerFileType.RDFXML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_XML_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.XML);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_XML_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testXlsToRDFXML() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String outFileName = "result.rdf.xml";
		Path outFilePath = tempDirPath.resolve(outFileName);
		SpdxConverter.convert(TEST_SPREADSHEET_XLS_FILE_PATH, outFilePath.toString(), SerFileType.XLS, SerFileType.RDFXML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_SPREADSHEET_XLS_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.XLS);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_SPREADSHEET_XLS_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testJsonToXls() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String xmlFileName = "result.rdf.xls";
		Path outFilePath = tempDirPath.resolve(xmlFileName);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString(), SerFileType.JSON, SerFileType.XLS);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_JSON_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.XLS);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.XLS);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testJsonToXlsx() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String xmlFileName = "result.rdf.xlsx";
		Path outFilePath = tempDirPath.resolve(xmlFileName);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString(), SerFileType.JSON, SerFileType.XLSX);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_JSON_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.XLSX);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.XLSX);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testJsonToTag() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String xmlFileName = "result.rdf.spdx";
		Path outFilePath = tempDirPath.resolve(xmlFileName);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString(), SerFileType.JSON, SerFileType.TAG);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_JSON_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.TAG);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.TAG);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testJsonToRdfXml() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String xmlFileName = "result.rdf.xml";
		Path outFilePath = tempDirPath.resolve(xmlFileName);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString(), SerFileType.JSON, SerFileType.RDFXML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_JSON_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.RDFXML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testJsonToYaml() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String xmlFileName = "result.yaml";
		Path outFilePath = tempDirPath.resolve(xmlFileName);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString(), SerFileType.JSON, SerFileType.YAML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_JSON_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.YAML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.YAML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testJsonToXml() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String xmlFileName = "result.xml";
		Path outFilePath = tempDirPath.resolve(xmlFileName);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString(), SerFileType.JSON, SerFileType.XML);
		File result = new File(outFilePath.toString());
		File source = new File(TEST_JSON_FILE_PATH);
		assertTrue(result.exists());
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocument(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.XML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocument(result, SerFileType.XML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}

}
