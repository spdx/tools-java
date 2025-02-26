/**
 * SPDX-FileCopyrightText: Copyright (c) 2020 Source Auditor Inc.
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
package org.spdx.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.spdx.core.DefaultModelStore;
import org.spdx.core.InvalidSPDXAnalysisException;
import org.spdx.core.ModelRegistry;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.library.model.v2.SpdxModelInfoV2_X;
import org.spdx.library.model.v3_0_1.SpdxModelInfoV3_0;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tools.SpdxToolsHelper.SerFileType;
import org.spdx.utility.compare.SpdxCompareException;
import org.spdx.utility.compare.SpdxComparer;

import junit.framework.TestCase;

/**
 * @author Gary O'Neall
 */
public class SpdxConverterTestV2 extends TestCase {
	
	static final String TEST_DIR = "testResources";
	static final String TEST_JSON_FILE_PATH = TEST_DIR + File.separator + "SPDXJSONExample-v2.3.spdx.json";
	static final String TEST_WITH_EXCEPTION_FILE_PATH = TEST_DIR + File.separator + "SPDXJSONExample-v2.3-with-exception.spdx.json";
	static final String TEST_RDF_FILE_PATH = TEST_DIR + File.separator + "SPDXRdfExample-v2.3.spdx.rdf";
	static final String TEST_SPREADSHEET_XLS_FILE_PATH = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.3.xls";
	static final String TEST_SPREADSHEET_XLSX_FILE_PATH = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.3.xlsx";
	static final String TEST_TAG_FILE_PATH = TEST_DIR + File.separator + "SPDXTagExample-v2.3.spdx";
	static final String TEST_XML_FILE_PATH = TEST_DIR + File.separator + "SPDXXMLExample-v2.3.spdx.xml";
	static final String TEST_YAML_FILE_PATH = TEST_DIR + File.separator + "SPDXYAMLExample-2.3.spdx.yaml";

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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.XLSX);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_SPREADSHEET_XLSX_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.TAG);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_TAG_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.YAML);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_YAML_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.XML);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_XML_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.XLS);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_SPREADSHEET_XLS_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.XLS);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.XLS);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.XLSX);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.XLSX);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.TAG);

		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.TAG);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.RDFXML);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.YAML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.YAML);
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
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.XML);
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
		
		// Try with no file types
		Files.delete(outFilePath);
		SpdxConverter.convert(TEST_JSON_FILE_PATH, outFilePath.toString());
		result = new File(outFilePath.toString());
		assertTrue(result.exists());
		resultDoc = SpdxToolsHelper.deserializeDocumentCompatV2(result, SerFileType.XML);
		comparer = new SpdxComparer();
		comparer.compare(sourceDoc, resultDoc);
		assertFalse(comparer.isDifferenceFound());
	}
	
	public void testLicenseDetailsRdf() throws SpdxConverterException, InvalidSPDXAnalysisException, IOException, SpdxCompareException {
		String detailsRdfFilePath = tempDirPath.resolve("details.rdf.xml").toString();
		String noDetailsRdfFilePath = tempDirPath.resolve("nodetails.rdf.xml").toString();
		SpdxConverter.convert(TEST_WITH_EXCEPTION_FILE_PATH, noDetailsRdfFilePath, SerFileType.JSON, SerFileType.RDFXML, true);
		File noDetailsFile = new File(noDetailsRdfFilePath);
		assertTrue(noDetailsFile.exists());
		SpdxConverter.convert(TEST_WITH_EXCEPTION_FILE_PATH, detailsRdfFilePath, SerFileType.JSON, SerFileType.RDFXML, false);
		File detailsFile = new File(detailsRdfFilePath);
		assertTrue(detailsFile.exists());
		File source = new File(TEST_WITH_EXCEPTION_FILE_PATH);
		SpdxDocument sourceDoc = SpdxToolsHelper.deserializeDocumentCompatV2(source, SerFileType.JSON);
		SpdxDocument detailsDoc = SpdxToolsHelper.deserializeDocumentCompatV2(detailsFile, SerFileType.RDFXML);
		SpdxDocument noDetailsDoc = SpdxToolsHelper.deserializeDocumentCompatV2(noDetailsFile, SerFileType.RDFXML);
		
		// Make sure they compare - the inclusion of the details should not impact of the 2 documents match
		SpdxComparer comparer = new SpdxComparer();
		comparer.compare(Arrays.asList(new SpdxDocument[] {sourceDoc, detailsDoc, noDetailsDoc}));
		assertFalse(comparer.isDifferenceFound());
		
		String mplLicenseUri = "http://spdx.org/licenses/MPL-1.0";
		String licenseTextProperty = "http://spdx.org/rdf/terms#licenseText";
		String exceptionUri = "http://spdx.org/licenses/Autoconf-exception-3.0";
		String exceptionTextProperty = "http://spdx.org/rdf/terms#licenseExceptionText";
		
		// Make sure the details has the details
		Model detailModel = ModelFactory.createDefaultModel();
		detailModel.read(detailsFile.toURI().toString());
		Resource detailMplLicense = detailModel.createResource(mplLicenseUri);
		Property detailLicenseTextProperty = detailModel.createProperty(licenseTextProperty);
		assertTrue(detailModel.contains(detailMplLicense, detailLicenseTextProperty));
		Resource detailException = detailModel.createResource(exceptionUri);
		Property detailExceptionTextProperty = detailModel.createProperty(exceptionTextProperty);
		assertTrue(detailModel.contains(detailException, detailExceptionTextProperty));
		
		// Make sure the noDetails does not have the listed licenses
		Model noDetailModel = ModelFactory.createDefaultModel();
		noDetailModel.read(noDetailsFile.toURI().toString());
		Resource noDetailMplLicense = noDetailModel.createResource(mplLicenseUri);
		assertFalse(noDetailModel.contains(noDetailMplLicense, detailLicenseTextProperty));
		Resource noDetailException = noDetailModel.createResource(exceptionUri);
		assertFalse(noDetailModel.contains(noDetailException, detailExceptionTextProperty));
	}
}
