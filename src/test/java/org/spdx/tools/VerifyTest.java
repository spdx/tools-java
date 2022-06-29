package org.spdx.tools;

import java.io.File;
import java.util.List;

import org.spdx.tools.SpdxToolsHelper.SerFileType;

import junit.framework.TestCase;

public class VerifyTest extends TestCase {
	
	static final String TEST_DIR = "testResources";
	static final String TEST_JSON_FILE_PATH = TEST_DIR + File.separator + "SPDXJSONExample-v2.3.spdx.json";
	static final String TEST_V23_FIELDS_IN_V22_FILE = TEST_DIR + File.separator + "SPDXWrongVersion.spdx.json";
	static final String TEST_RDF_FILE_PATH = TEST_DIR + File.separator + "SPDXRdfExample-v2.3.spdx.rdf";
	static final String TEST_SPREADSHEET_XLS_FILE_PATH = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.3.xls";
	static final String TEST_SPREADSHEET_XLSX_FILE_PATH = TEST_DIR + File.separator + "SPDXSpreadsheetExample-v2.3.xlsx";
	static final String TEST_TAG_FILE_PATH = TEST_DIR + File.separator + "SPDXTagExample-v2.3.spdx";
	static final String TEST_XML_FILE_PATH = TEST_DIR + File.separator + "SPDXXMLExample-v2.3.spdx.xml";
	static final String TEST_YAML_FILE_PATH = TEST_DIR + File.separator + "SPDXYAMLExample-2.3.spdx.yaml";
	static final String TEST_WARNING_FILE_PATH = TEST_DIR + File.separator + "SPDXTagExample-v2.2-warning.spdx";
    static final String BAD_JSON_FILE_PATH = TEST_DIR + File.separator + "BadJSON.spdx.json";
	
	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testUpsupportedVersionFields() throws SpdxVerificationException {
		List<String> result = Verify.verify(TEST_V23_FIELDS_IN_V22_FILE, SerFileType.JSON);
		assertTrue(result.size() > 0);
	}
	
	public void testVerifyTagFile() throws SpdxVerificationException {
		List<String> result = Verify.verifyTagFile(TEST_TAG_FILE_PATH);
		assertEquals(0, result.size());
	}

	public void testVerifyRDFFile() throws SpdxVerificationException {
		List<String> result = Verify.verifyRDFFile(TEST_RDF_FILE_PATH);
		assertEquals(0, result.size());
	}

	public void testVerify() throws SpdxVerificationException {
		List<String> result = Verify.verify(TEST_JSON_FILE_PATH, SerFileType.JSON);
		assertEquals(0, result.size());
		result = Verify.verify(TEST_SPREADSHEET_XLS_FILE_PATH, SerFileType.XLS);
		assertEquals(0, result.size());
		result = Verify.verify(TEST_SPREADSHEET_XLSX_FILE_PATH, SerFileType.XLSX);
		assertEquals(0, result.size());
		result = Verify.verify(TEST_XML_FILE_PATH, SerFileType.XML);
		assertEquals(0, result.size());
		result = Verify.verify(TEST_YAML_FILE_PATH, SerFileType.YAML);
		assertEquals(0, result.size());
	}
	
	public void testVerifyWarning() throws SpdxVerificationException {
		List<String> result = Verify.verify(TEST_WARNING_FILE_PATH, SerFileType.TAG);
		assertTrue(result.size() > 0);
		assertTrue(result.get(0).contains("deprecated"));
	}
	
	public void testVerifyBadJSON() throws SpdxVerificationException {
		List<String> result = Verify.verify(BAD_JSON_FILE_PATH, SerFileType.JSON);
		assertTrue(result.size() == 5);
	}
}
