/**
 * SPDX-FileCopyrightText: 2026 SPDX Contributors
 * SPDX-FileType: SOURCE
 * SPDX-License-Identifier: Apache-2.0
 */

package org.spdx.tools;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.spdx.core.DefaultModelStore;
import org.spdx.library.ModelCopyManager;
import org.spdx.library.SpdxModelFactory;
import org.spdx.library.model.v2.SpdxDocument;
import org.spdx.storage.simple.InMemSpdxStore;
import org.spdx.tools.SpdxToolsHelper.SerFileType;

import junit.framework.TestCase;

/**
 * Test conversion between SPDX versions using SpdxConverter
 *
 * @author Arthit Suriyawongkul
 */
public class SpdxConverterVersionTest extends TestCase {

	static final String TEST_DIR = "testResources";
	static final String TEST_V22_JSON = TEST_DIR + File.separator
			+ "SPDXJSONExample-v2.2.spdx.json";
	static final String TEST_V23_JSON = TEST_DIR + File.separator
			+ "SPDXJSONExample-v2.3.spdx.json";

	Path tempDirPath;

	@Before
	public void setUp() throws Exception {
		SpdxModelFactory.init();
		DefaultModelStore.initialize(
				new InMemSpdxStore(),
				"http://default/namespace",
				new ModelCopyManager());
		tempDirPath = Files.createTempDirectory("spdx-tools-test-");
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		SpdxConverterTestV3.deleteDirAndFiles(tempDirPath);
	}

	public void testUpgradeV22ToV23() throws Exception {
		String outputFileName = "result-v2.3.json";
		Path outFilePath = tempDirPath.resolve(outputFileName);
		
		// Run converter specifying --toVersion 2.3
		SpdxConverter.convert(TEST_V22_JSON, outFilePath.toString(), "2.3");
		
		File resultFile = outFilePath.toFile();
		assertTrue(resultFile.exists());
		
		// Deserialize and verify spec version is SPDX-2.3
		SpdxDocument resultDoc = SpdxToolsHelper
				.deserializeDocumentCompatV2(resultFile, SerFileType.JSON);
		assertEquals("SPDX-2.3", resultDoc.getSpecVersion());
	}

	public void testUpgradeV22ToV301() throws Exception {
		String outputFileName = "result-v3.jsonld";
		Path outFilePath = tempDirPath.resolve(outputFileName);
		
		// Run converter specifying --toVersion 3.0.1 (JSONLD output type is
		// auto-detected from extension)
		SpdxConverter.convert(TEST_V22_JSON, outFilePath.toString(), "3.0.1");
		
		File resultFile = outFilePath.toFile();
		assertTrue(resultFile.exists());
		
		// Deserialize and verify spec version is 3.0.1
		org.spdx.library.model.v3_0_1.core.SpdxDocument resultDoc = SpdxToolsHelper
				.deserializeDocument(resultFile, SerFileType.JSONLD);
		assertEquals("3.0.1", resultDoc.getSpecVersion());
	}

	public void testRejectDowngradeV23ToV22() throws Exception {
		String outputFileName = "result-downgrade.json";
		Path outFilePath = tempDirPath.resolve(outputFileName);
		
		// Call run(...) directly with args to verify command line exit codes
		// and messaging
		String[] args = new String[] {
				TEST_V23_JSON,
				outFilePath.toString(),
				"--toVersion",
				"2.2"
		};
		
		int exitCode = SpdxConverter.run(args);
		assertEquals(ExitCode.USAGE_ERROR, exitCode);
		assertFalse(outFilePath.toFile().exists());
	}

	public void testRejectInvalidVersion() throws Exception {
		String outputFileName = "result-invalid.json";
		Path outFilePath = tempDirPath.resolve(outputFileName);
		
		String[] args = new String[] {
				TEST_V22_JSON,
				outFilePath.toString(),
				"--toVersion",
				"foo"
		};
		
		int exitCode = SpdxConverter.run(args);
		assertEquals(ExitCode.USAGE_ERROR, exitCode);
		assertFalse(outFilePath.toFile().exists());
	}

	public void testV3DowngradeRejection() throws Exception {
		// V3 to V2 downgrade check in SpdxConverter.java:
		// fromVersion == SpdxMajorVersion.VERSION_3 && toVersion !=
		// SpdxMajorVersion.VERSION_3
		// Let's verify that we reject converting from a V3 JSON-LD file to a V2
		// JSON file
		String v3Input = TEST_DIR + File.separator
				+ "SPDXJsonLDExample-v3.0.1.json";
		String outputFileName = "result-v2.json";
		Path outFilePath = tempDirPath.resolve(outputFileName);
		
		String[] args = new String[] {
				v3Input,
				outFilePath.toString(),
				"JSONLD",
				"JSON"
		};
		
		int exitCode = SpdxConverter.run(args);
		// SpdxConverterException is caught, prints message, and returns
		// ExitCode.ERROR (1)
		assertEquals(ExitCode.ERROR, exitCode);
		assertFalse(outFilePath.toFile().exists());
	}
}
