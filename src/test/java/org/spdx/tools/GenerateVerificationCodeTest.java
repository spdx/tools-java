package org.spdx.tools;

import java.io.File;

import org.spdx.library.InvalidSPDXAnalysisException;
import org.spdx.library.model.SpdxPackageVerificationCode;

import junit.framework.TestCase;

public class GenerateVerificationCodeTest extends TestCase {
	
	static final String TEST_DIR = "testResources";
	static final String FILES = TEST_DIR + File.separator + "sourcefiles";

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGenerateVerificationCode() throws OnlineToolException, InvalidSPDXAnalysisException {
		String skippedRegex = "Ver.*";
		SpdxPackageVerificationCode result = GenerateVerificationCode.generateVerificationCode(FILES, skippedRegex);
		assertFalse(result.getValue().isEmpty());
		assertEquals(1, result.getExcludedFileNames().size());
		assertTrue(result.getExcludedFileNames().contains("./VerificationSheet.java"));
		SpdxPackageVerificationCode result2 = GenerateVerificationCode.generateVerificationCode(FILES, null);
		assertFalse(result.equivalent(result2));
		assertEquals(0, result2.getExcludedFileNames().size());
	}

}
